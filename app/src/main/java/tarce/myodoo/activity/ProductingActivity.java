package tarce.myodoo.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.newland.me.ConnUtils;
import com.newland.me.DeviceManager;
import com.newland.mtype.ConnectionCloseEvent;
import com.newland.mtype.ModuleType;
import com.newland.mtype.event.DeviceEventListener;
import com.newland.mtype.module.common.printer.Printer;
import com.newland.mtypex.nseries.NSConnV100ConnParams;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Response;
import tarce.api.MyCallback;
import tarce.api.RetrofitClient;
import tarce.api.api.InventoryApi;
import tarce.model.LoginResponse;
import tarce.model.inventory.CommonBean;
import tarce.model.inventory.GetFactroyRemarkBean;
import tarce.model.inventory.OrderDetailBean;
import tarce.myodoo.R;
import tarce.myodoo.adapter.DoneAdapter;
import tarce.myodoo.adapter.product.OrderDetailAdapter;
import tarce.myodoo.uiutil.DialogForOrder;
import tarce.myodoo.uiutil.FullyLinearLayoutManager;
import tarce.myodoo.uiutil.InsertFeedbackDial;
import tarce.myodoo.uiutil.InsertNumDialog;
import tarce.myodoo.uiutil.TipDialog;
import tarce.myodoo.utils.StringUtils;
import tarce.myodoo.utils.UserManager;
import tarce.support.AlertAialogUtils;
import tarce.support.MyLog;
import tarce.support.SharePreferenceUtils;
import tarce.support.TimeUtils;
import tarce.support.ToastUtils;
import tarce.support.ToolBarActivity;

/**
 * Created by rose.zou on 2017/5/31.
 * 生产中详情页面
 */

public class ProductingActivity extends ToolBarActivity {
    private static final String K21_DRIVER_NAME = "com.newland.me.K21Driver";
    @InjectView(R.id.tv_state_order)
    TextView tvStateOrder;
    @InjectView(R.id.img_up_down)
    ImageView imgUpDown;
    @InjectView(R.id.tv_check_state)
    TextView tvCheckState;
    @InjectView(R.id.tv_name_product)
    TextView tvNameProduct;
    @InjectView(R.id.tv_time_product)
    TextView tvTimeProduct;
    @InjectView(R.id.tv_rework_product)
    TextView tvReworkProduct;
    @InjectView(R.id.tv_num_product)
    TextView tvNumProduct;
    @InjectView(R.id.tv_need_num)
    TextView tvNeedNum;
    @InjectView(R.id.tv_gongxu_product)
    TextView tvGongxuProduct;
    @InjectView(R.id.tv_type_product)
    TextView tvTypeProduct;
    @InjectView(R.id.recycler_order_detail)
    RecyclerView recyclerOrderDetail;
    @InjectView(R.id.recycler2_order_detail)
    RecyclerView recycler2OrderDetail;
    @InjectView(R.id.recycler3_order_detail)
    RecyclerView recycler3OrderDetail;
    @InjectView(R.id.tv_start_produce)
    TextView tvStartProduce;
    @InjectView(R.id.tv_add_ll)
    TextView tvAddLl;
    @InjectView(R.id.tv_product_out)
    TextView tvProductOut;
    @InjectView(R.id.tv_person_manage)
    TextView tvPersonManage;
    @InjectView(R.id.tv_line_stop)
    TextView tvLineStop;
    @InjectView(R.id.tv_string_guige)
    TextView tvStringGuige;
    @InjectView(R.id.relative_order_show)
    RelativeLayout relativeOrderShow;
    @InjectView(R.id.linear_three)
    LinearLayout linearThree;
    /*@InjectView(R.id.tv_print_string)
    TextView tvPrintString;*/
    @InjectView(R.id.eidt_mo_note)
    EditText eidtMoNote;
    @InjectView(R.id.edit_sale_note)
    EditText editSaleNote;
    @InjectView(R.id.recycler_done)
    RecyclerView recyclerDone;
    @InjectView(R.id.produce_line_id)
    TextView produceLineId;
    @InjectView(R.id.tv_second_product)
    TextView tvSecondProduct;
    @InjectView(R.id.tv_product_finish)
    TextView tvProductFinish;
    @InjectView(R.id.tv_feed_material)
    TextView tvFeedMaterial;
    private int order_id;
    private String state;
    private int limit;
    private String delay_state;
    private int process_id;
    private String name_activity;
    private String state_activity;
    private InventoryApi inventoryApi;
    private OrderDetailBean.ResultBean.ResDataBean.PrepareMaterialAreaIdBean prepare_material_area_id;
    private String prepare_material_img;
    private OrderDetailBean.ResultBean.ResDataBean resDataBean;
    private List<OrderDetailBean.ResultBean.ResDataBean.StockMoveLinesBean> list_one;
    private List<OrderDetailBean.ResultBean.ResDataBean.StockMoveLinesBean> list_two;
    private List<OrderDetailBean.ResultBean.ResDataBean.StockMoveLinesBean> list_three;
    private OrderDetailBean.ResultBean result;
    private OrderDetailAdapter adapter;
    private OrderDetailAdapter adapter_two;
    private OrderDetailAdapter adapter_three;
    private DialogForOrder dialogForOrder;
    private boolean isShowDialog = true;
    private InsertNumDialog insertNumDialog;
    private boolean product_line = true;
    private boolean up_or_down = true;//判断是收起还是展开,默认展开
    private DeviceManager deviceManager;
    private Printer printer;
    private String order_name;
    private boolean is_rework;
    private DoneAdapter doneAdapter;
    private int production_line_id;
    private LoginResponse userInfoBean;
    private int allow_produced_qty_rate;
    private int origin_sale_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producting);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        order_id = intent.getIntExtra("order_id", 1);
        order_name = intent.getStringExtra("order_name");
        setTitle(order_name);
        state = intent.getStringExtra("state");
        limit = intent.getIntExtra("limit", 1);
        delay_state = intent.getStringExtra("delay_state");
        process_id = intent.getIntExtra("process_id", 1);
        name_activity = intent.getStringExtra("name_activity");
        state_activity = intent.getStringExtra("state_activity");
        origin_sale_id = intent.getIntExtra("origin_sale_id", 0);
        if (state.equals("progress")) {
            production_line_id = intent.getIntExtra("production_line_id", 100);
        }

        stateView(state);
        userInfoBean = UserManager.getSingleton().getUserInfoBean();
        if (userInfoBean!=null){
            allow_produced_qty_rate = userInfoBean.getResult().getRes_data().getAllow_produced_qty_rate();
        }
        recyclerOrderDetail.setLayoutManager(new FullyLinearLayoutManager(ProductingActivity.this));
        recyclerOrderDetail.addItemDecoration(new DividerItemDecoration(ProductingActivity.this,
                DividerItemDecoration.VERTICAL));
        recycler2OrderDetail.setLayoutManager(new FullyLinearLayoutManager(ProductingActivity.this));
        recycler2OrderDetail.addItemDecoration(new DividerItemDecoration(ProductingActivity.this,
                DividerItemDecoration.VERTICAL));
        recycler3OrderDetail.setLayoutManager(new FullyLinearLayoutManager(ProductingActivity.this));
        recycler3OrderDetail.addItemDecoration(new DividerItemDecoration(ProductingActivity.this,
                DividerItemDecoration.VERTICAL));
        recyclerDone.setLayoutManager(new FullyLinearLayoutManager(ProductingActivity.this));
        recyclerDone.addItemDecoration(new DividerItemDecoration(ProductingActivity.this,
                DividerItemDecoration.VERTICAL));
        recyclerOrderDetail.setNestedScrollingEnabled(false);
        recycler2OrderDetail.setNestedScrollingEnabled(false);
        recycler3OrderDetail.setNestedScrollingEnabled(false);
        recyclerDone.setNestedScrollingEnabled(false);
        showDefultProgressDialog();
        getDetail();
    }

    @Override
    protected void onResume() {
        if (resDataBean == null && result == null) {
            getDetail();
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (state.equals("waiting_material") || state.equals("prepare_material_ing")) {
            menu.getItem(2).setTitle("备料反馈");
        } else {
            menu.getItem(2).setTitle("生产反馈");
        }
        return true;
    }

    /**
     * 根据state显示不同布局
     */
    private void stateView(String state) {
        switch (state) {
            case "draft":
                tvStateOrder.setText("草稿");
                break;
            case "confirmed":
                tvStateOrder.setText("已排产");
                break;
            case "waiting_material":
                tvStateOrder.setText("等待备料");
                tvStartProduce.setText("开始备料");
                showLinThreePro();
                break;
            case "prepare_material_ing":
                tvStateOrder.setText("备料中");
                tvStartProduce.setText("备料完成");
                showLinThreePro();
                break;
            case "finish_prepare_material":
                tvStateOrder.setText("备料完成");
                break;
            case "already_picking":
                tvStateOrder.setText("已领料");
                break;
            case "planned":
                tvStateOrder.setText("安排");
                break;
            case "progress":
                tvStateOrder.setText("进行中");
                showLinThreePro();
                break;
            case "waiting_inspection_finish":
                tvStateOrder.setText("等待品检完成");
                break;
            case "waiting_rework":
                tvStateOrder.setText("等待返工");
                break;
            case "rework_ing":
                tvStateOrder.setText("返工中");
                break;
            case "waiting_inventory_material":
                tvStateOrder.setText("等待清点退料");
                break;
            case "waiting_warehouse_inspection":
                tvStateOrder.setText("等待检验退料");
                break;
            case "waiting_post_inventory":
                tvStateOrder.setText("等待入库");
                break;
            case "done":
                tvStateOrder.setText("完成");
                break;
        }
    }

    /**
     * 订单详情
     */
    private void getDetail() {
        inventoryApi = RetrofitClient.getInstance(ProductingActivity.this).create(InventoryApi.class);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("order_id", order_id);
        Call<OrderDetailBean> orderDetail = inventoryApi.getOrderDetail(hashMap);
        orderDetail.enqueue(new MyCallback<OrderDetailBean>() {
            @Override
            public void onResponse(Call<OrderDetailBean> call, Response<OrderDetailBean> response) {
                dismissDefultProgressDialog();
                if (response.body() == null) return;
                if (response.body().getError() != null) {
                    new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, response.body().getError().getData().getMessage())
                            .show();
                    return;
                }
                if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                    result = response.body().getResult();
                    resDataBean = response.body().getResult().getRes_data();
                    prepare_material_area_id = response.body().getResult().getRes_data().getPrepare_material_area_id();
                    prepare_material_img = response.body().getResult().getRes_data().getPrepare_material_img();
                    is_rework = resDataBean.getProcess_id().isIs_rework();
                    initView();
                } else {
                    //ToastUtils.showCommonToast(ProductingActivity.this, "数据异常");
                    Log.e("zws", "数据异常");
                }
            }

            @Override
            public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                dismissDefultProgressDialog();
                ToastUtils.showCommonToast(ProductingActivity.this, t.toString());
                Log.e("zws", t.toString());
            }
        });
    }

    /**
     * 是否显示底部（生产）
     */
    public void showLinThreePro() {
        if (!UserManager.getSingleton().getGrops().contains("group_charge_produce")) {
            linearThree.setVisibility(View.GONE);
        }
    }

    /**
     * 根据数据赋值显示view
     */
    private void initView() {
        if (resDataBean.is_secondary_produce()) {
            tvSecondProduct.setText("二次生产");
        } else {
            tvSecondProduct.setVisibility(View.GONE);
        }
        if (resDataBean.getProduction_line_id() != null) {
            produceLineId.setText("产线：" + resDataBean.getProduction_line_id().getName());
        } else {
            produceLineId.setText("产线暂无");
        }
        tvNameProduct.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        if (resDataBean.getProduct_name().equals("false")) {
            tvNameProduct.setText("\n");
        } else {
            tvNameProduct.setText(resDataBean.getProduct_name());
        }
        double num_product = resDataBean.getQty_produced();
        tvNumProduct.setText(num_product+"");
        if (num_product > 0) {
            tvStartProduce.setVisibility(View.VISIBLE);
        }
        tvNeedNum.setText(resDataBean.getProduct_qty()+"");
        tvTimeProduct.setText(TimeUtils.utc2Local(resDataBean.getDate_planned_start()));
        tvReworkProduct.setText(resDataBean.getIn_charge_name());
        tvStringGuige.setText(String.valueOf(resDataBean.getProduct_id().getProduct_specs()));
        tvStringGuige.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        eidtMoNote.setText(resDataBean.getRemark());
        editSaleNote.setText(resDataBean.getSale_remark());
        switch (String.valueOf(resDataBean.getProduction_order_type())) {
            case "stockup":
                tvTypeProduct.setText("备货制");
                break;
            case "ordering":
                tvTypeProduct.setText("订单制");
                break;
        }
        tvGongxuProduct.setText(resDataBean.getProcess_id().getName());
        list_one = new ArrayList<>();
        list_two = new ArrayList<>();
        list_three = new ArrayList<>();
        for (int i = 0; i < resDataBean.getStock_move_lines().size(); i++) {
            if (resDataBean.getStock_move_lines().get(i).getProduct_type().equals("material")) {
                list_one.add(resDataBean.getStock_move_lines().get(i));
            } else if (resDataBean.getStock_move_lines().get(i).getProduct_type().equals("real_semi_finished")) {
                list_two.add(resDataBean.getStock_move_lines().get(i));
            } else {
                list_three.add(resDataBean.getStock_move_lines().get(i));
            }
        }
        adapter = new OrderDetailAdapter(ProductingActivity.this, list_one, "原材料", result);
        adapter_two = new OrderDetailAdapter(ProductingActivity.this, list_two, "半成品", result);
        adapter_three = new OrderDetailAdapter(ProductingActivity.this, list_three, "流转品", result);
        recyclerOrderDetail.setAdapter(adapter);
        recycler2OrderDetail.setAdapter(adapter_two);
        recycler3OrderDetail.setAdapter(adapter_three);
        // TODO: 2017/9/28 针对DIY多产出
//        if (resDataBean.getProcess_id().is_multi_output() || resDataBean.getProcess_id().is_random_output()) {
//            doneAdapter = new DoneAdapter(R.layout.item_done_adapter, resDataBean.getDone_stock_moves(), false);
//            recyclerDone.setAdapter(doneAdapter);
//            tvProductFinish.setVisibility(View.VISIBLE);
//            for (int i = 0; i < resDataBean.getDone_stock_moves().size(); i++) {
//                if (resDataBean.getDone_stock_moves().get(i).getQuantity_done_finished()>0){
//                    tvStartProduce.setVisibility(View.VISIBLE);
//                    break;
//                }
//            }
//        }
    }

    /**
     * 点击产出
     */
    @OnClick(R.id.tv_product_out)
    void outProduct(View view) {
        //随意产出和多产出特殊处理
        if (resDataBean == null) return;
//        if (resDataBean.getProcess_id()!=null && (resDataBean.getProcess_id().is_multi_output() || resDataBean.getProcess_id().is_random_output())) {
//            Intent intent = new Intent(ProductingActivity.this, MoreProduceActicity.class);
//            intent.putExtra("bean", (Serializable) resDataBean.getDone_stock_moves());
//            intent.putExtra("id", order_id);
//            startActivity(intent);
//        } else {
        try {
            insertNumDialog = new InsertNumDialog(ProductingActivity.this, R.style.MyDialogStyle,
                    new InsertNumDialog.OnSendCommonClickListener() {
                        @Override
                        public void OnSendCommonClick(final double num) {
                            boolean isCanAdd = false;
                            if (is_rework || resDataBean.is_secondary_produce()) {
                                isCanAdd = true;
                            } else {
                                for (int i = 0; i < resDataBean.getStock_move_lines().size(); i++) {
                                    double bomProduct = (num + resDataBean.getQty_produced()) / resDataBean.getProduct_qty() * resDataBean.getStock_move_lines().get(i)
                                            .getProduct_uom_qty();
                                    if (bomProduct <= (resDataBean.getStock_move_lines().get(i).getQuantity_done() -
                                            resDataBean.getStock_move_lines().get(i).getReturn_qty())*(1+allow_produced_qty_rate*0.01)) {//增加  减去退料数量
                                        isCanAdd = true;
                                    } else {
                                        if (resDataBean.getProcess_id().getName().equals("返工")) {
                                            isCanAdd = true;
                                            return;
                                        }
                                        AlertAialogUtils.getCommonDialog(ProductingActivity.this, "")
                                                .setMessage(resDataBean.getStock_move_lines().get(i).getProduct_id() + "备料数量不足，请补料")
                                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                }).show();
                                        isCanAdd = false;
                                        break;
                                    }
                                }
                            }
                            if (isCanAdd) {
                                showDefultProgressDialog();
                                HashMap<Object, Object> hashMap = new HashMap<>();
                                hashMap.put("order_id", order_id);
                                hashMap.put("produce_qty", num);
                                Call<OrderDetailBean> objectCall = inventoryApi.checkOut(hashMap);
                                objectCall.enqueue(new MyCallback<OrderDetailBean>() {
                                    @Override
                                    public void onResponse(Call<OrderDetailBean> call, Response<OrderDetailBean> response) {
                                        dismissDefultProgressDialog();
                                        if (response.body() == null) return;
                                        if (response.body().getError() != null) {
                                            new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, response.body().getError().getData().getMessage())
                                                    .show();
                                            return;
                                        }
                                        if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                                            resDataBean = response.body().getResult().getRes_data();
                                            new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, "提示\n\n本次产出成功，目前生产结果如下\n\n需求数量：" +
                                                    resDataBean.getProduct_qty() + "\n\n生产数量：" + response.body().getResult().getRes_data()
                                                    .getQty_produced())
                                                    .show();
                                            tvStartProduce.setVisibility(View.VISIBLE);
                                            tvNumProduct.setText(response.body().getResult().getRes_data()
                                                    .getQty_produced()+"");
                                        } else if (response.body().getResult().getRes_data() != null && response.body().getResult().getRes_code() == -1) {
                                            ToastUtils.showCommonToast(ProductingActivity.this, response.body().getResult().getRes_data().getError());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                                        //  ToastUtils.showCommonToast(ProductingActivity.this, t.toString());
                                        dismissDefultProgressDialog();
                                    }
                                });
                            }
                        }
                    }, resDataBean.getProduct_name());
            insertNumDialog.show();
        } catch (Exception e) {
            ToastUtils.showCommonToast(ProductingActivity.this, e.toString());
            Log.e("zws", e.toString());
        }
        // }
    }

    /**
     * 新增的退料按钮点击时间
     */
    @OnClick(R.id.tv_feed_material)
    void clickFeedMaterial(View view) {
        try {
            Intent intent3 = new Intent(ProductingActivity.this, WriteFeedMateriActivity.class);
            intent3.putExtra("recycler_data", resDataBean);
            intent3.putExtra("order_id", order_id);
            intent3.putExtra("from", "anytimeProduct");
            intent3.putExtra("origin_sale_id", origin_sale_id);
            startActivity(intent3);
        } catch (Exception e) {
            ToastUtils.showCommonToast(ProductingActivity.this, e.toString());
        }
    }

    /**
     * 点击补领料
     */
    @OnClick(R.id.tv_add_ll)
    void addLl(View view) {
        try {
            Intent intent = new Intent(ProductingActivity.this, BuGetLiaoActivity.class);
            intent.putExtra("value", resDataBean);
            intent.putExtra("state", resDataBean.getState());
            intent.putExtra("order_id", order_id);
            startActivity(intent);
        } catch (Exception e) {
            ToastUtils.showCommonToast(ProductingActivity.this, "数据暂时丢失，请重试");
            Log.e("zws", e.toString());
        }
    }

    /**
     * 点击规格显示更详细内容
     */
    @OnClick(R.id.tv_string_guige)
    void showDetail(View view) {
        new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, String.valueOf(resDataBean.getProduct_id().getProduct_specs()))
                .show();
    }

    /**
     * 点击人员管理
     */
    @OnClick(R.id.tv_person_manage)
    void managePerson(View view) {
        try {
            Intent intent = new Intent(ProductingActivity.this, AddPersonActivity.class);
            intent.putExtra("order_id", order_id);
            intent.putExtra("state_activity", state_activity);
            intent.putExtra("name_activity", name_activity);
            intent.putExtra("close", true);
            startActivity(intent);
        } catch (Exception e) {
            ToastUtils.showCommonToast(ProductingActivity.this, e.toString());
        }
    }

    /**
     * 点击产线暂停
     */
    @OnClick(R.id.tv_line_stop)
    void stopLine(View view) {
        if (product_line) {
            AlertAialogUtils.getCommonDialog(ProductingActivity.this, "")
                    .setMessage("是否确定暂停产线")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopProductLine("outline", 1);
                        }
                    }).show();
        } else {
            AlertAialogUtils.getCommonDialog(ProductingActivity.this, "")
                    .setMessage("是否确定恢复产线")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopProductLine("online", 0);
                        }
                    }).show();
        }
    }

    /**
     * 暂停产线  恢复产线
     */
    private void stopProductLine(String state, int is_all_pending) {
        showDefultProgressDialog();
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("order_id", order_id);
        hashMap.put("state", state);
        hashMap.put("is_all_pending", is_all_pending);
        Call<OrderDetailBean> objectCall = inventoryApi.stopProductLine(hashMap);
        objectCall.enqueue(new MyCallback<OrderDetailBean>() {
            @Override
            public void onResponse(Call<OrderDetailBean> call, Response<OrderDetailBean> response) {
                dismissDefultProgressDialog();
                if (response.body() == null) return;
                if (response.body().getError() != null) {
                    new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, response.body().getError().getData().getMessage())
                            .show();
                    return;
                }
                if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                    if (product_line) {
                        tvLineStop.setText("恢复产线");
                        product_line = false;
                    } else {
                        tvLineStop.setText("产线暂停");
                        product_line = true;
                    }
                } else {
                    //    ToastUtils.showCommonToast(ProductingActivity.this, "出现错误，请联系开发人员调试");
                    Log.e("zws", "数据异常");
                }
            }

            @Override
            public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                dismissDefultProgressDialog();
                ToastUtils.showCommonToast(ProductingActivity.this, t.toString());
            }
        });
    }

    /**
     * 生产完成
     */
    @OnClick(R.id.tv_start_produce)
    void finishProduct(View view) {
        AlertAialogUtils.getCommonDialog(ProductingActivity.this, "")
                .setTitle("是否确定生产完成，目前生产结果如下：")
                .setMessage("本单总需求数量：" + tvNeedNum.getText().toString() + "\n本单总生产数量：" + tvNumProduct.getText().toString())
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDefultProgressDialog();
                        HashMap<Object, Object> hashMap = new HashMap<>();
                        hashMap.put("order_id", order_id);
                        Call<OrderDetailBean> objectCall = inventoryApi.finishProduct(hashMap);
                        objectCall.enqueue(new MyCallback<OrderDetailBean>() {
                            @Override
                            public void onResponse(Call<OrderDetailBean> call, final Response<OrderDetailBean> response) {
                                dismissDefultProgressDialog();
                                if (response.body() == null) return;
                                if (response.body().getError() != null) {
                                    new TipDialog(ProductingActivity.this, R.style.MyDialogStyle, response.body().getError().getData().getMessage())
                                            .show();
                                    return;
                                }
                                if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ProgressDialog progressDialog = new ProgressDialog(ProductingActivity.this);
                                                    progressDialog.setMessage("正在打印...");
                                                    progressDialog.show();
                                                    printPra();
                                                    progressDialog.dismiss();
                                                }
                                            });
                                        }
                                    }).start();
                                    AlertAialogUtils.getCommonDialog(ProductingActivity.this, "生产完成，是否拍摄产品位置信息")
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(ProductingActivity.this, PhotoAreaActivity.class);
                                                    intent.putExtra("type", state);
                                                    intent.putExtra("order_id", order_id);
//                                                    intent.putExtra("delay_state", delay_state);
//                                                    intent.putExtra("limit", limit);
                                                    intent.putExtra("process_id", process_id);
                                                    intent.putExtra("origin_sale_id", origin_sale_id);
                                                    intent.putExtra("change", true);
                                                    intent.putExtra("bean", response.body().getResult().getRes_data());
                                                    if (state.equals("progress")) {
                                                        intent.putExtra("production_line_id", production_line_id);
                                                    }
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            })
                                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
//                                                    Intent intent = new Intent(ProductingActivity.this, ProductLineListActivity.class);
//                                                    intent.putExtra("name_activity", "生产中");
//                                                    intent.putExtra("state_product", state);
//                                                    intent.putExtra("process_id", process_id);
//                                                    intent.putExtra("production_line_id", production_line_id);
//                                                    Log.e("zws", "production_line_id="+production_line_id);
//                                                    startActivity(intent);
                                                    finish();
                                                }
                                            }).show();
                                } else if (response.body().getResult().getRes_data() != null && response.body().getResult().getRes_code() == -1) {
                                    ToastUtils.showCommonToast(ProductingActivity.this, response.body().getResult().getRes_data().getError());
                                }
                            }

                            @Override
                            public void onFailure(Call<OrderDetailBean> call, Throwable t) {
                                dismissDefultProgressDialog();
                                Log.e("zws", t.toString());
                            }
                        });
                    }
                }).show();
    }

    /**
     * 收起，展开
     */
    @OnClick(R.id.img_up_down)
    void onClickImage(View v) {
        if (up_or_down) {
            relativeOrderShow.setVisibility(View.GONE);
            tvCheckState.setText("展开");
            imgUpDown.setImageResource(R.mipmap.down);
            up_or_down = false;
        } else {
            relativeOrderShow.setVisibility(View.VISIBLE);
            tvCheckState.setText("收起");
            imgUpDown.setImageResource(R.mipmap.up);
            up_or_down = true;
        }
    }

    /**
     * 点击查看BOM结构
     */
    @OnClick(R.id.tv_name_product)
    void bomDetail(View view) {
        Intent intent = new Intent(ProductingActivity.this, BomFramworkActivity.class);
        intent.putExtra("order_id", order_id);
        startActivity(intent);
    }

    /**
     * 连接设备打印机
     */
    private void initDevice() {
        deviceManager = ConnUtils.getDeviceManager();
        try {
            deviceManager.init(ProductingActivity.this, K21_DRIVER_NAME, new NSConnV100ConnParams(), new DeviceEventListener<ConnectionCloseEvent>() {
                @Override
                public void onEvent(ConnectionCloseEvent connectionCloseEvent, Handler handler) {
                    if (connectionCloseEvent.isSuccess()) {
                        ToastUtils.showCommonToast(ProductingActivity.this, "设备被客户主动断开！");
                    }
                    if (connectionCloseEvent.isFailed()) {
                        ToastUtils.showCommonToast(ProductingActivity.this, "设备链接异常断开！");
                    }
                }

                @Override
                public Handler getUIHandler() {
                    return null;
                }
            });
            deviceManager.connect();
            MyLog.e("zws", "连接成功");
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showCommonToast(ProductingActivity.this, "链接异常,请检查设备或重新连接.." + e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == tarce.support.R.id.action_settings) {
            HashMap<Object, Object> hashMap = new HashMap<>();
            hashMap.put("order_id", order_id);
            Call<GetFactroyRemarkBean> factroyRemark = inventoryApi.getFactroyRemark(hashMap);
            factroyRemark.enqueue(new MyCallback<GetFactroyRemarkBean>() {
                @Override
                public void onResponse(Call<GetFactroyRemarkBean> call, Response<GetFactroyRemarkBean> response) {
                    if (response.body() == null || response.body().getResult() == null) return;
                    if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data() != null) {
                        String remark = response.body().getResult().getRes_data().getFactory_mark();
                        new InsertFeedbackDial(ProductingActivity.this, R.style.MyDialogStyle, new InsertFeedbackDial.OnSendCommonClickListener() {
                            @Override
                            public void OnSendCommonClick(String num) {
                                HashMap<Object, Object> hashMap = new HashMap<>();
                                hashMap.put("factory_remark", num);
                                hashMap.put("order_id", order_id);
                                Call<CommonBean> objectCall = inventoryApi.updateFactroyRemark(hashMap);
                                objectCall.enqueue(new MyCallback<CommonBean>() {
                                    @Override
                                    public void onResponse(Call<CommonBean> call, Response<CommonBean> response) {
                                        if (response == null) return;
                                        if (response.body().getResult().getRes_code() == 1) {
                                            ToastUtils.showCommonToast(ProductingActivity.this, "反馈成功");
                                        }
                                    }
                                });
                            }
                        }, remark).show();
                    } else {
                        //ToastUtils.showCommonToast(ProductingActivity.this, "出现错误，请联系开发人员调试");
                        Log.e("zws", "数据异常");
                    }
                }

                @Override
                public void onFailure(Call<GetFactroyRemarkBean> call, Throwable t) {
                    ToastUtils.showCommonToast(ProductingActivity.this, t.toString());
                }
            });
        } else if (item.getItemId() == tarce.support.R.id.action_print) {
            AlertAialogUtils.getCommonDialog(ProductingActivity.this, "是否确认打印？\n(请尽量避免订单重复打印)")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            printPra();
                        }
                    })
                    .show();
        } else if (item.getItemId() == R.id.action_feedback) {
            // ToastUtils.showCommonToast(ProductingActivity.this, "此功能仅适用于备料阶段");
            Intent intent = new Intent(ProductingActivity.this, FeedbackActivity.class);
            intent.putExtra("state", state);
            intent.putExtra("order_id", order_id);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        resDataBean = null;
        result = null;
        super.onPause();
    }

    private void printPra() {
        initDevice();
        printer = (Printer) deviceManager.getDevice().getStandardModule(ModuleType.COMMON_PRINTER);
        printer.init();
        printer.setLineSpace(1);
        printer.print("\nMO单号：" + order_name + "\n" + "产品: " + tvNameProduct.getText() + "\n" + "时间： " + tvTimeProduct.getText() + "\n" +
                "负责人: " + tvReworkProduct.getText() + "\n" + "生产数量：" + tvNumProduct.getText() + "\n" + "需求数量：" + tvNeedNum.getText()
                + "\n" + "规格：" + tvStringGuige.getText() + "\n" + "工序：" + tvGongxuProduct.getText() + "\n" + "类型：" + tvTypeProduct.getText()
                + "\n" + "MO单备注：" + eidtMoNote.getText() + "\n" + "销售单备注：" + editSaleNote.getText() + "\n", 30, TimeUnit.SECONDS);
        Bitmap mBitmap = CodeUtils.createImage(order_name, 150, 150, null);
        printer.print(0, mBitmap, 30, TimeUnit.SECONDS);
        printer.print("\n\n\n\n\n\n\n", 30, TimeUnit.SECONDS);
    }
}
