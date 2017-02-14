
package bruce.sun.vr.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import bruce.sun.vr.R;

public abstract class GlassModeDialog extends Dialog implements View.OnClickListener {
    private Context context;

    private TextView messageTextView;

    private TextView leftTimeTextView;

    private TextView buyBtnTextView;

    private MyHandler mHandler;

    private int leftTime = 5;

    private volatile boolean hasClickBuy;

    public GlassModeDialog(Context context) {
        super(context);
        init(context);

    }

    public GlassModeDialog(Context context, int theme) {
        super(context, theme);
        init(context);
    }

    public GlassModeDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    @SuppressWarnings("deprecation")
    private void init(Context context) {
        this.setCanceledOnTouchOutside(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.glass_mode_dialog);
        mHandler = new MyHandler(this);
        getWindow().setBackgroundDrawableResource(R.drawable.round_border);
        if (!(context instanceof Activity)) {
            getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        this.context = context;

        messageTextView = (TextView) findViewById(R.id.dialog_message_title);
        leftTimeTextView = (TextView) findViewById(R.id.dialog_left_time_textview);
        buyBtnTextView = (TextView) findViewById(R.id.dialog_buy_btn_textview);
        buyBtnTextView.setOnClickListener(this);

//        String switchInfo = BaofengSwitchUtil.getMojingBuyGlass(context);
//        if(!"1".equals(switchInfo)){
//            buyBtnTextView.setVisibility(View.GONE);
//        }

        timeUpdate();
    }

    private synchronized void timeUpdate(){
        if(hasClickBuy){
            return;
        }
        if(leftTime <= 0){
            continuePlay();
        }else{
            messageTextView.setText(context.getString(R.string.glass_mode_dialog_title,leftTime+""));
            leftTimeTextView.setText(leftTime+"");
            if(mHandler != null){
                mHandler.sendEmptyMessageDelayed(0, 1000);
            }
            leftTime= leftTime -1;
        }
    }



    @Override
    public void dismiss() {
        // TODO Auto-generated method stub
        super.dismiss();
        hasClickBuy = true;
    }

    @Override
    public synchronized void onClick(View v) {
        if(v.getId() == R.id.dialog_buy_btn_textview){
            hasClickBuy = true;
            leftTime = 1000;
            mHandler.removeMessages(0);
            mHandler  = null;
            gotoBuyClick();
        }
    }

    public abstract void gotoBuyClick();

    public abstract void continuePlay();

    protected static class MyHandler extends Handler {
        WeakReference<GlassModeDialog> reference;

        MyHandler(GlassModeDialog dialog) {
            reference = new WeakReference<GlassModeDialog>(dialog);
        }
        public void handleMessage(Message msg) {
            if (reference == null || reference.get() == null) {
                return;
            }
            GlassModeDialog dialog = reference.get();
            if (dialog == null) {
                return;
            }

            switch (msg.what) {
                case 0:
                   dialog.timeUpdate();
                    break;
            }
        }
    }
}
