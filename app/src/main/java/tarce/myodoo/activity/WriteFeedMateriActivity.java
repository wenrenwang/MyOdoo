package tarce.myodoo.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.newland.me.ConnUtils;
import com.newland.me.DeviceManager;
import com.newland.mtype.ConnectionCloseEvent;
import com.newland.mtype.ModuleType;
import com.newland.mtype.event.DeviceEventListener;
import com.newland.mtype.module.common.rfcard.RFCardModule;
import com.newland.mtype.module.common.rfcard.RFResult;
import com.newland.mtype.util.ISOUtils;
import com.newland.mtypex.nseries.NSConnV100ConnParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import tarce.api.MyCallback;
import tarce.api.OKHttpFactory;
import tarce.api.RetrofitClient;
import tarce.api.api.InventoryApi;
import tarce.model.inventory.GetReturnMaterBean;
import tarce.model.inventory.NfcOrderBean;
import tarce.model.inventory.OrderDetailBean;
import tarce.myodoo.R;
import tarce.myodoo.activity.salesout.SalesDetailActivity;
import tarce.myodoo.adapter.product.WriteFeedAdapter;
import tarce.myodoo.adapter.product.WriteFeedbackNumAdapter;
import tarce.myodoo.device.Const;
import tarce.myodoo.uiutil.DialogIsSave;
import tarce.myodoo.uiutil.InsertNumDialog;
import tarce.myodoo.uiutil.NFCdialog;
import tarce.support.AlertAialogUtils;
import tarce.support.MyLog;
import tarce.support.ToastUtils;
import tarce.support.ToolBarActivity;

import static tarce.api.RetrofitClient.Url;

/**
 * Created by rose.zou on 2017/6/5.
 * 填写退料页面
 */

public class WriteFeedMateriActivity extends ToolBarActivity {
    private static final String K21_DRIVER_NAME = "com.newland.me.K21Driver";
    @InjectView(R.id.recycler_feed_material)
    RecyclerView recyclerFeedMaterial;
    @InjectView(R.id.tv_commit_feednum)
    TextView tvCommitFeednum;
    private OrderDetailBean.ResultBean.ResDataBean resDataBean;
    private WriteFeedbackNumAdapter adapter;
    private InsertNumDialog insertNumDialog;
    private InventoryApi inventoryApi;
    private int order_id;
    private String from;
    private WriteFeedAdapter feedAdapter;
    private List<GetReturnMaterBean.ResultBean.ResDataBean> res_data;
    private DeviceManager deviceManager;
    private RFCardModule rfCardModule;
    private NFCdialog nfCdialog;
    private Retrofit retrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_feedmater);
        ButterKnife.inject(this);

        setRecyclerview(recyclerFeedMaterial);
        retrofit = new Retrofit.Builder()
                //设置OKHttpClient
                .client(new OKHttpFactory(WriteFeedMateriActivity.this).getOkHttpClient())
                .baseUrl(Url+"/linkloving_user_auth/")
                //gson转化器
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        Url = RetrofitClient.Url;
        Intent intent = getIntent();
        resDataBean = (OrderDetailBean.ResultBean.ResDataBean) intent.getSerializableExtra("recycler_data");
        order_id = intent.getIntExtra("order_id", 1);
        from = intent.getStringExtra("from");
        if (from.equals("look")) {
            tvCommitFeednum.setText("确认退料数量");
        }
        inventoryApi = RetrofitClient.getInstance(WriteFeedMateriActivity.this).create(InventoryApi.class);
        initData();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        if (from.equals("write") || from.equals("check")) {
            List<OrderDetailBean.ResultBean.ResDataBean.StockMoveLinesBean> stock_move_lines = resDataBean.getStock_move_lines();
            feedAdapter = new WriteFeedAdapter(R.layout.adapter_write_feednum, stock_move_lines);
            recyclerFeedMaterial.setAdapter(feedAdapter);
            feedAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(final BaseQuickAdapter adapter, View view, final int position) {
                    insertNumDialog = new InsertNumDialog(WriteFeedMateriActivity.this, R.style.MyDialogStyle,
                            new InsertNumDialog.OnSendCommonClickListener() {
                                public double beiNum;//备料数量

                                @Override
                                public void OnSendCommonClick(int num) {
                                    if (resDataBean.getState().equals("waiting_material")
                                            || resDataBean.getState().equals("prepare_material_ing")
                                            || resDataBean.getState().equals("finish_prepare_material")) {
                                        beiNum = resDataBean.getStock_move_lines().get(position).getQuantity_ready() + resDataBean.getStock_move_lines().get(position).getQuantity_done();
                                    } else {
                                        beiNum = resDataBean.getStock_move_lines().get(position).getQuantity_done();
                                    }
                                    // TODO: 2017/6/8 生产num/需求num*item的需求num
                                    double v = resDataBean.getQty_produced() / resDataBean.getProduct_qty() * resDataBean.getStock_move_lines().get(position).getProduct_uom_qty();
                                    if (num <= (beiNum - v)) {
                                        resDataBean.getStock_move_lines().get(position).setReturn_qty(num);
                                        feedAdapter.notifyDataSetChanged();
                                    } else {
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "退料过多");
                                    }
                                }
                            }, resDataBean.getStock_move_lines().get(position).getProduct_id(), position, resDataBean)
                            .changeTitle("输入 " + resDataBean.getStock_move_lines().get(position).getProduct_id() + " 的退料数量");
                    insertNumDialog.show();
                }
            });
        } else {
            showDefultProgressDialog();
            HashMap<Object, Object> hashMap = new HashMap();
            hashMap.put("order_id", order_id);
            Call<GetReturnMaterBean> returnMater = inventoryApi.getReturnMater(hashMap);
            returnMater.enqueue(new MyCallback<GetReturnMaterBean>() {
                @Override
                public void onResponse(Call<GetReturnMaterBean> call, Response<GetReturnMaterBean> response) {
                    dismissDefultProgressDialog();
                    if (response.body() == null || response.body().getResult() == null) return;
                    if (response.body().getResult().getRes_data() != null && response.body().getResult().getRes_code() == 1) {
                        res_data = response.body().getResult().getRes_data();
                        adapter = new WriteFeedbackNumAdapter(R.layout.adapter_write_feednum, response.body().getResult().getRes_data());
                        recyclerFeedMaterial.setAdapter(adapter);
                        initRecyc();
                    }
                }
                @Override
                public void onFailure(Call<GetReturnMaterBean> call, Throwable t) {
                    dismissDefultProgressDialog();
                    ToastUtils.showCommonToast(WriteFeedMateriActivity.this, t.toString());
                }
            });
        }
    }

    /**
     * 设置recycler
     */
    private void initRecyc() {

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final BaseQuickAdapter adapter, View view, final int position) {
                insertNumDialog = new InsertNumDialog(WriteFeedMateriActivity.this, R.style.MyDialogStyle,
                        new InsertNumDialog.OnSendCommonClickListener() {
                            public double beiNum;//备料数量

                            @Override
                            public void OnSendCommonClick(final int num) {
                                if (resDataBean.getState().equals("waiting_material")
                                        || resDataBean.getState().equals("prepare_material_ing")
                                        || resDataBean.getState().equals("finish_prepare_material")) {
                                    beiNum = resDataBean.getStock_move_lines().get(position).getQuantity_ready() + resDataBean.getStock_move_lines().get(position).getQuantity_done();
                                } else {
                                    beiNum = resDataBean.getStock_move_lines().get(position).getQuantity_done();
                                }
                                // TODO: 2017/6/8 生产num/需求num*item的需求num
                                double v = resDataBean.getQty_produced() / resDataBean.getProduct_qty() * resDataBean.getStock_move_lines().get(position).getProduct_uom_qty();
                                if (num <= (beiNum - v)) {
                                    String product_type = resDataBean.getStock_move_lines().get(position).getProduct_type();
                                    if (product_type.equals("material") || product_type.equals("real_semi_finished")){
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                initDevice();
                                                processingLock();
                                                showNfcDialog();
                                                try {
                                                    final RFResult qPResult = rfCardModule.powerOn(null, 10, TimeUnit.SECONDS);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (qPResult.getCardSerialNo() == null) {
                                                                ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "不能识别序列号：" + Const.MessageTag.DATA);
                                                            } else {
                                                                showDefultProgressDialog();
                                                                String NFC_Number = ISOUtils.hexString(qPResult.getCardSerialNo());
                                                                InventoryApi inventory = retrofit.create(InventoryApi.class);
                                                                HashMap<Object, Object> hashMap = new HashMap<>();
                                                                hashMap.put("card_num", NFC_Number);
                                                                Call<NfcOrderBean> objectCall = inventory.authWarehouse(hashMap);
                                                                objectCall.enqueue(new Callback<NfcOrderBean>() {
                                                                    @Override
                                                                    public void onResponse(Call<NfcOrderBean> call, Response<NfcOrderBean> response) {
                                                                        dismissDefultProgressDialog();
                                                                        if (response.body() == null) return;
                                                                        if (response.body().getError() != null) {
                                                                            nfCdialog.setHeaderImage(R.drawable.warning)
                                                                                    .setTip(response.body().getError().getData().getMessage())
                                                                                    .setCancelVisi().show();
                                                                            threadDismiss(nfCdialog);
                                                                        } else if (response.body().getResult() != null && response.body().getResult().getRes_code() == -1) {
                                                                            nfCdialog.setHeaderImage(R.drawable.warning)
                                                                                    .setTip(response.body().getResult().getRes_data().getErrorX())
                                                                                    .setCancelVisi().show();
                                                                            threadDismiss(nfCdialog);
                                                                        } else if (response.body().getResult() != null && response.body().getResult().getRes_code() == 1) {
                                                                            final NfcOrderBean.ResultBean.ResDataBean res_dataNfc = response.body().getResult().getRes_data();
                                                                            nfCdialog.setHeaderImage(R.drawable.defaultimage)
                                                                                    .setTip(res_dataNfc.getName() + res_dataNfc.getEmployee_id() + "\n" + res_dataNfc.getWork_email()
                                                                                            + "\n\n" + "打卡成功")
                                                                                    .setCancelVisi().show();
                                                                            threadDismiss(nfCdialog);
                                                                            res_data.get(position).setReturn_qty(num);
                                                                            res_data.get(position).setNfc(true);
                                                                            adapter.notifyDataSetChanged();
                                                                        }
                                                                    }
                                                                    @Override
                                                                    public void onFailure(Call<NfcOrderBean> call, Throwable t) {
                                                                        dismissDefultProgressDialog();
                                                                        Log.e("zws", t.toString());
                                                                    }
                                                                });
                                                            }
                                                            processingUnLock();
                                                        }
                                                    });
                                                } catch (final Exception e) {
                                                    e.fillInStackTrace();
                                                    if (e.getMessage().equals("device invoke timeout!7")) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    Thread.sleep(1000);
                                                                    ToastUtils.showCommonToast(WriteFeedMateriActivity.this, e.getMessage() + "  " + Const.MessageTag.ERROR);
                                                                    nfCdialog.dismiss();
                                                                } catch (InterruptedException e1) {
                                                                    e1.printStackTrace();
                                                                }
                                                            }
                                                        });
                                                    }
                                                    processingUnLock();
                                                }
                                            }
                                        }).start();
                                    }else {
                                        res_data.get(position).setReturn_qty(num);
                                        res_data.get(position).setNfc(true);
                                        adapter.notifyDataSetChanged();
                                    }
                                } else {
                                    ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "退料过多");
                                }
                            }
                        }, resDataBean.getStock_move_lines().get(position).getProduct_id(), position, resDataBean)
                        .changeTitle("输入 " + resDataBean.getStock_move_lines().get(position).getProduct_id() + " 的退料数量");
                insertNumDialog.show();
            }
        });
    }

    //显示nfc的dialog
    private void showNfcDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nfCdialog = new NFCdialog(WriteFeedMateriActivity.this);
                nfCdialog.setCancel(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        processingUnLock();
                        nfCdialog.dismiss();
                        return;
                    }
                }).show();
            }
        });
    }

    //关闭dialog
    private void threadDismiss(final NFCdialog dialog) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 1000);
    }

    @OnClick(R.id.tv_commit_feednum)
    void commitNum(View view) {
        if (from.equals("check") || from.equals("write")) {
            AlertAialogUtils.getCommonDialog(WriteFeedMateriActivity.this, "确定提交？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showDefultProgressDialog();
                            HashMap<Object, Object> hashMap = new HashMap();
                            hashMap.put("order_id", order_id);
                            hashMap.put("is_check", 0);
                            Map[] maps = new Map[resDataBean.getStock_move_lines().size()];
                            for (int i = 0; i < resDataBean.getStock_move_lines().size(); i++) {
                                Map<Object, Object> smallMap = new HashMap<>();
                                smallMap.put("order_id", resDataBean.getStock_move_lines().get(i).getOrder_id());
                                smallMap.put("product_tmpl_id", resDataBean.getStock_move_lines().get(i).getProduct_tmpl_id());
                                smallMap.put("return_qty", feedAdapter.getData().get(i).getReturn_qty());
                                maps[i] = smallMap;
                            }
                            hashMap.put("stock_moves", maps);
                            Call<OrderDetailBean> objectCall = inventoryApi.semiCommitReturn(hashMap);
                            objectCall.enqueue(new MyCallback<OrderDetailBean>() {
                                @Override
                                public void onResponse(Call<OrderDetailBean> call, Response<OrderDetailBean> response) {
                                    dismissDefultProgressDialog();
                                    if (response.body() == null || response.body().getResult() == null) return;
                                    if (response.body().getError() != null) {
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, response.body().getError().getMessage());
                                        return;
                                    }
                                    if (response.body().getResult().getRes_data() != null && response.body().getResult().getRes_code() == 1) {
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "提交退料成功");
                                        Intent intent = new Intent(WriteFeedMateriActivity.this, ProductLlActivity.class);
                                        intent.putExtra("name_activity", "生产退料");
                                        if (from.equals("check")) {
                                            intent.putExtra("state_product", "done");
                                        } else if (from.equals("write")) {
                                            intent.putExtra("state_product", "waiting_inventory_material");
                                        }
                                        startActivity(intent);
                                        finish();
                                    } else if (response.body().getResult().getRes_data() != null && response.body().getResult().getRes_code() == -1){
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, response.body().getResult().getRes_data().getError());
                                    } else {
                                        //ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "数据错误");
                                        Log.e("zws", "数据异常");
                                    }
                                }

                                @Override
                                public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                                    dismissDefultProgressDialog();
                                    ToastUtils.showCommonToast(WriteFeedMateriActivity.this, t.toString());
                                }
                            });
                        }
                    }).show();
        } else {
            boolean pass = false;
            for (int i = 0; i < res_data.size(); i++) {
                if (!res_data.get(i).isNfc()){
                    pass = true;
                    break;
                }
            }
            if (pass){
                ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "有未确认的退料");
                return;
            }
            AlertAialogUtils.getCommonDialog(WriteFeedMateriActivity.this, "是否确定提交")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showDefultProgressDialog();
                            HashMap<Object, Object> hashMap = new HashMap();
                            hashMap.put("order_id", order_id);
                            if (from.equals("look")) {
                                hashMap.put("is_check", 1);
                            } else {
                                hashMap.put("is_check", 0);
                            }
                            Map[] maps = new Map[resDataBean.getStock_move_lines().size()];
                            for (int i = 0; i < resDataBean.getStock_move_lines().size(); i++) {
                                Map<Object, Object> smallMap = new HashMap<>();
                                smallMap.put("order_id", resDataBean.getStock_move_lines().get(i).getOrder_id());
                                smallMap.put("product_tmpl_id", resDataBean.getStock_move_lines().get(i).getProduct_tmpl_id());
                                smallMap.put("return_qty", adapter.getData().get(i).getReturn_qty());
                                maps[i] = smallMap;
                            }
                            hashMap.put("stock_moves", maps);
                            Call<OrderDetailBean> objectCall = inventoryApi.commitFeedNum(hashMap);
                            objectCall.enqueue(new MyCallback<OrderDetailBean>() {
                                @Override
                                public void onResponse(Call<OrderDetailBean> call, Response<OrderDetailBean> response) {
                                    dismissDefultProgressDialog();
                                    if (response.body() == null || response.body().getResult() == null) return;
                                    if (response.body().getError() != null) {
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, response.body().getError().getMessage());
                                        return;
                                    }
                                    if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                                        Intent intent = new Intent(WriteFeedMateriActivity.this, ProductLlActivity.class);
                                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "退料完成");
                                        intent.putExtra("name_activity", "生产退料");
                                        intent.putExtra("state_product", "waiting_warehouse_inspection");
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        //ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "数据错误");
                                        Log.e("zws", "数据异常");
                                    }
                                }

                                @Override
                                public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                                    dismissDefultProgressDialog();
                                    ToastUtils.showCommonToast(WriteFeedMateriActivity.this, t.toString());
                                }
                            });
                        }
                    }).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (from.equals("look") && res_data!=null) {
            if (item.getItemId() == android.R.id.home) {
                boolean isBack = false;
                for (int i = 0; i < res_data.size(); i++) {
                    if (res_data.get(i).isNfc()) {
                        isBack = true;
                        break;
                    }
                }
                if (isBack) {
                    isBack = false;
                    AlertAialogUtils.getCommonDialog(WriteFeedMateriActivity.this, "已经有确认过的退料了，是否确认返回？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).show();
                }else {
                    finish();
                }
                return isBack;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 返回按钮
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (from.equals("look") && res_data!=null) {
            if (keyCode == KeyEvent.KEYCODE_BACK) { //监控/拦截/屏蔽返回键
                boolean isBack = false;
                for (int i = 0; i < res_data.size(); i++) {
                    if (res_data.get(i).isNfc()) {
                        isBack = true;
                        break;
                    }
                }
                if (isBack) {
                    isBack = false;
                    AlertAialogUtils.getCommonDialog(WriteFeedMateriActivity.this, "已经有确认过的退料了，是否确认返回？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).show();
                }else {
                    finish();
                }
                return isBack;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 连接设备打印机
     */
    private void initDevice() {
        deviceManager = ConnUtils.getDeviceManager();
        try {
            deviceManager.init(WriteFeedMateriActivity.this, K21_DRIVER_NAME, new NSConnV100ConnParams(), new DeviceEventListener<ConnectionCloseEvent>() {
                @Override
                public void onEvent(ConnectionCloseEvent connectionCloseEvent, Handler handler) {
                    if (connectionCloseEvent.isSuccess()) {
                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "设备被客户主动断开！");
                    }
                    if (connectionCloseEvent.isFailed()) {
                        ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "设备链接异常断开！");
                    }
                }

                @Override
                public Handler getUIHandler() {
                    return null;
                }
            });
            deviceManager.connect();
            MyLog.e("OrderDetailActivity", "连接成功");
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showCommonToast(WriteFeedMateriActivity.this, "链接异常,请检查设备或重新连接.." + e);
        }
        rfCardModule = (RFCardModule) deviceManager.getDevice().getStandardModule(ModuleType.COMMON_RFCARDREADER);
    }

    public void processingLock() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                SharedPreferences setting = getSharedPreferences("setting", 0);
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean("PBOC_LOCK", true);
                editor.commit();
            }
        });

    }

    public void processingUnLock() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences setting = getSharedPreferences("setting", 0);
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean("PBOC_LOCK", false);
                editor.commit();
            }
        });

    }
}
