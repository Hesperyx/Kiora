package cn.hxy.kiora.hook.debug

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Button
import android.widget.EditText
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.findField
import cn.hxy.kiora.utils.reflect.newInstanceWithArgs
import com.tencent.mobileqq.msf.service.MsfService
import com.tencent.mobileqq.sign.QQSecuritySign
import com.tencent.qphone.base.remote.ToServiceMsg
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import java.lang.ref.WeakReference

/** 获取测试签名，移植自 TCQT GetSign（已裁剪依赖 libtcqtmem 的 source32 扫描）。 */
@HookItemAnnotation(
    "获取测试签名",
    "本功能仅用于测试，正常情况下无需启用！用法：在聊天框随便打个字符，然后长按发送按钮即可获取签名",
    HookCategory.DEBUG,
    "All"
)
object GetSign : BaseSwitchHookItem(), DexKitTask {

    private var pendingEditText: WeakReference<EditText>? = null
    private var pendingSendBtn: WeakReference<Button>? = null

    private val signer by lazy {
        val method = requireMethod(TASK_GET_SIGN)
        method.declaringClass.newInstanceWithArgs() to method
    }

    override fun onHook() {
        when (HostInfo.processName) {
            HostInfo.packageName -> initMainProcess()
            "${HostInfo.packageName}:msf" -> initMsfProcess()
        }
    }

    // ------------------------------------------------------------------
    // 主进程：长按发送按钮触发取签名
    // ------------------------------------------------------------------
    private fun initMainProcess() {
        val method = runCatching { requireMethod(TASK_INPUT_ROOT_INIT) }.getOrNull()
            ?: runCatching { requireMethod(TASK_INPUT_ROOT_INIT_FALLBACK) }.getOrNull()
            ?: return

        method.hookAfter(this) { param ->
            val sendBtn = runCatching {
                param.thisObject.javaClass
                    .findField { type = Button::class.java }
                    .get(param.thisObject) as? Button
            }.getOrNull() ?: return@hookAfter

            val editText = runCatching {
                param.thisObject.javaClass
                    .findField { type = EditText::class.java }
                    .get(param.thisObject) as? EditText
            }.getOrNull() ?: return@hookAfter

            sendBtn.setOnLongClickListener {
                onBtnLongClick(sendBtn, editText)
                true
            }
        }

        val resultReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {
                val error = intent.getStringExtra("error")

                if (error != null) {
                    getTargetEditText()?.setText("签名获取失败: $error")
                    restoreSendBtn()
                    clearPendingViews()
                    return
                }

                val sign = intent.getStringExtra("sign") ?: return

                getTargetEditText()?.setText("${HostInfo.versionName} $sign")
                restoreSendBtn()
                clearPendingViews()
            }
        }

        registerReceiver(resultReceiver, IntentFilter(ACTION_SIGN_RESULT))
    }

    @SuppressLint("SetTextI18n")
    private fun onBtnLongClick(sendBtn: Button, editText: EditText) {
        pendingEditText = WeakReference(editText)

        val userInput = editText.text?.toString() ?: ""
        val cmd = if (userInput.trim().isNotEmpty()) {
            userInput
        } else {
            "MessageSvc.PbSendMsg"
        }

        Intent(ACTION_REQUEST_SIGN).apply {
            putExtra("uin", QQCurrentEnv.currentUin)
            putExtra("cmd", cmd)
            setPackage(HostInfo.packageName)
        }.also {
            HostInfo.hostContext.sendBroadcast(it)
        }

        editText.postDelayed({
            val pendingEdit = pendingEditText?.get()
            if (pendingEdit === editText) {
                pendingEditText = null
                editText.isEnabled = true
            }

            val pendingButton = pendingSendBtn?.get()
            if (pendingButton === sendBtn) {
                pendingSendBtn = null
                sendBtn.isEnabled = true
            }
        }, REQUEST_TIMEOUT_MS)
    }

    // ------------------------------------------------------------------
    // MSF 进程：计算签名并回传
    // ------------------------------------------------------------------
    private fun initMsfProcess() {
        val requestReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                runCatching {
                    val uin = intent.getStringExtra("uin") ?: "0"
                    val cmd = intent.getStringExtra("cmd") ?: "MessageSvc.PbSendMsg"

                    val buffer =
                        "000000160A08120608D48BCAE5031206080110001800".hexToByteArray()

                    val seq = MsfService.getCore().nextSeq

                    val toServiceMsg = ToServiceMsg("mobileqq.service", uin, cmd).apply {
                        putWupBuffer(buffer)
                        setRequestSsoSeq(seq)
                    }

                    val (instance, method) = signer
                    val signResult = method.invoke(instance, toServiceMsg, cmd)
                    val sign = (signResult as? QQSecuritySign.SignResult)
                        ?.sign
                        ?.toHexString()
                        .orEmpty()

                    Intent(ACTION_SIGN_RESULT).apply {
                        putExtra("sign", sign)
                        setPackage(context.packageName)
                    }.also {
                        context.sendBroadcast(it)
                    }
                }.onFailure { e ->
                    LogUtils.e("GetSign", e)

                    Intent(ACTION_SIGN_RESULT).apply {
                        putExtra("error", e.message ?: "unknown error")
                        setPackage(context.packageName)
                    }.also {
                        context.sendBroadcast(it)
                    }
                }
            }
        }

        registerReceiver(requestReceiver, IntentFilter(ACTION_REQUEST_SIGN))
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        runCatching {
            val context = HostInfo.hostContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }.onFailure { LogUtils.e("GetSign", it) }
    }

    private fun getTargetEditText(): EditText? {
        return pendingEditText?.get() ?: getAIOEditText()
    }

    private fun restoreSendBtn() {
        pendingSendBtn?.get()?.isEnabled = true
        getTargetEditText()?.isEnabled = true
    }

    private fun clearPendingViews() {
        pendingEditText = null
        pendingSendBtn = null
    }

    @SuppressLint("DiscouragedApi")
    private fun getAIOEditText(): EditText? {
        return runCatching {
            QQCurrentEnv.activity?.let { activity ->
                val resId = activity.resources.getIdentifier("input", "id", activity.packageName)
                if (resId != 0) {
                    activity.findViewById<EditText>(resId)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        TASK_INPUT_ROOT_INIT to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.aio.input.simpleui")
            matcher {
                usingEqStrings(
                    "binding",
                    "inputRoot",
                    "findViewById(...)",
                    "getContext(...)",
                    "sendBtn",
                )
            }
        },
        TASK_INPUT_ROOT_INIT_FALLBACK to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.aio.input.simpleui")
            matcher {
                usingEqStrings("inputRoot.findViewById(R.id.send_btn)")
            }
        },
        TASK_GET_SIGN to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.msf.core")
            matcher {
                usingEqStrings("invoke getSign start", "invoke getSign end")
            }
        }
    )

    private const val ACTION_REQUEST_SIGN = "cn.hxy.kiora.GET_SIGN_REQUEST"
    private const val ACTION_SIGN_RESULT = "cn.hxy.kiora.GET_SIGN_RESULT"

    private const val TASK_INPUT_ROOT_INIT = "InputRootInit"
    private const val TASK_INPUT_ROOT_INIT_FALLBACK = "InputRootInitFallback"
    private const val TASK_GET_SIGN = "getSign"

    private const val REQUEST_TIMEOUT_MS = 30_000L
}
