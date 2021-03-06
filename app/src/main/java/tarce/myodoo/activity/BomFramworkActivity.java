package tarce.myodoo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.zaihuishou.expandablerecycleradapter.adapter.BaseExpandableAdapter;
import com.zaihuishou.expandablerecycleradapter.viewholder.AbstractAdapterItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit2.Call;
import retrofit2.Response;
import tarce.api.MyCallback;
import tarce.api.RetrofitClient;
import tarce.api.api.InventoryApi;
import tarce.model.inventory.BomFramworkBean;
import tarce.model.inventory.BomSubBean;
import tarce.model.inventory.SevenBomBean;
import tarce.myodoo.R;
import tarce.myodoo.adapter.expand.CompanyItem;
import tarce.myodoo.adapter.expand.DepartmentItem;
import tarce.myodoo.adapter.expand.EmployeeItem;
import tarce.myodoo.adapter.expand.LastItem;
import tarce.myodoo.adapter.expand.SevenItem;
import tarce.myodoo.adapter.expand.SixItem;
import tarce.myodoo.adapter.expand.WorkerItem;
import tarce.myodoo.uiutil.TipDialog;
import tarce.support.MyLog;
import tarce.support.ToastUtils;
import tarce.support.ToolBarActivity;

/**
 * Created by rose.zou on 2017/6/6.
 * Bom结构表
 */

public class BomFramworkActivity extends BaseActivity {
    @InjectView(R.id.recycler_rom_framwork)
    RecyclerView recyclerRomFramwork;
    private InventoryApi inventoryApi;
    private int order_id;
    private BaseExpandableAdapter adapter;
    private final int ITEM_TYPE_COMPANY = 1;
    private final int ITEM_TYPE_DEPARTMENT = 2;
    private final int ITEM_TYPE_EMPLOYEE = 3;
    private final int ITEM_TYPE_WORKER = 4;
    private final int ITEM_TYPE_LAST = 5;
    private final int ITEM_TYPE_SIX = 6;
    private final int ITEM_TYPE_SEVEN = 7;
    private List<Object> mCompanylist;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bom_framwork);
        ButterKnife.inject(this);

        setRecyclerview(recyclerRomFramwork);
        setTitle("BOM结构");

        Intent intent = getIntent();
        order_id = intent.getIntExtra("order_id", 1);
        getData();
    }


    private void getData() {
        showDefultProgressDialog();
        inventoryApi = RetrofitClient.getInstance(BomFramworkActivity.this).create(InventoryApi.class);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("order_id", order_id);
        Call<BomFramworkBean> bomDetail = inventoryApi.getBomDetail(hashMap);
        bomDetail.enqueue(new MyCallback<BomFramworkBean>(){
            @Override
            public void onResponse(final Call<BomFramworkBean> call, Response<BomFramworkBean> response) {
                dismissDefultProgressDialog();
                if (response.body() == null)return;
                if (response.body().getError()!=null){
                    new TipDialog(BomFramworkActivity.this, R.style.MyDialogStyle, response.body().getError().getData().getMessage())
                            .show();
                    return;
                }
                if (response.body().getResult().getRes_code() == 1 && response.body().getResult().getRes_data()!=null){
                    final BomFramworkBean.ResultBean.ResDataBean res_data = response.body().getResult().getRes_data();
                    mCompanylist = new ArrayList<>();
                    mCompanylist.add(createCompany(res_data, false));
                    adapter = new BaseExpandableAdapter(mCompanylist){

                        @Override
                        public AbstractAdapterItem<Object> getItemView(Object type) {
                            int itemType = (int) type;
                            switch (itemType){
                                case ITEM_TYPE_COMPANY:
                                    return new CompanyItem();
                                case ITEM_TYPE_DEPARTMENT:
                                    return new DepartmentItem(BomFramworkActivity.this);
                                case ITEM_TYPE_EMPLOYEE:
                                    return new EmployeeItem(BomFramworkActivity.this);
                                case ITEM_TYPE_WORKER:
                                    return new WorkerItem(BomFramworkActivity.this);
                                case ITEM_TYPE_LAST:
                                    return new LastItem(BomFramworkActivity.this);
                                case ITEM_TYPE_SIX:
                                    return new SixItem(BomFramworkActivity.this);
                                case ITEM_TYPE_SEVEN:
                                    return new SevenItem(BomFramworkActivity.this);
                            }
                            return null;
                        }

                        @Override
                        public Object getItemViewType(Object t){
                            if (t instanceof BomFramworkBean.ResultBean.ResDataBean) {
                                return ITEM_TYPE_COMPANY;
                            } else if (t instanceof BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX)
                                return ITEM_TYPE_DEPARTMENT;
                            else if (t instanceof BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX.BomIdsBean)
                                return ITEM_TYPE_EMPLOYEE;
                            else if (t instanceof BomSubBean)
                                return ITEM_TYPE_WORKER;
                            else if (t instanceof BomSubBean.BomBottomBean)
                                return ITEM_TYPE_LAST;
                            else if (t instanceof BomSubBean.BomBottomBean.SixBomBottomBean)
                                return ITEM_TYPE_SIX;
                            else if (t instanceof SevenBomBean)
                                return ITEM_TYPE_SEVEN;
                            return -1;
                        }
                    };
                    recyclerRomFramwork.setAdapter(adapter);
                }else {
                  //  ToastUtils.showCommonToast(BomFramworkActivity.this, "出现错误，请联系开发人员调试");
                    Log.e("zws", "数据异常");
                }

            }

            @Override
            public void onFailure(Call<BomFramworkBean> call, Throwable t){
                dismissDefultProgressDialog();
                ToastUtils.showCommonToast(BomFramworkActivity.this, t.toString());
                Log.e("zws", t.toString());
            }
        });
    }

    private BomFramworkBean.ResultBean.ResDataBean createCompany(BomFramworkBean.ResultBean.ResDataBean resDataBean, boolean isExpandDefault) {
        BomFramworkBean.ResultBean.ResDataBean firstCompany = new BomFramworkBean.ResultBean.ResDataBean();
        firstCompany.name = resDataBean.getName();
        firstCompany.code = resDataBean.getCode();
        firstCompany.process_id = resDataBean.getProcess_id();
        firstCompany.product_specs = resDataBean.getProduct_specs();
        List<BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX> departments = new ArrayList<>();
        for (int i = 0; i < resDataBean.getBom_ids().size(); i++){
            BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX department = new BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX();
            BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX bomIdsBeanX = resDataBean.getBom_ids().get(i);
            department.name = bomIdsBeanX.getName();
            department.code = bomIdsBeanX.getCode();
            department.product_specs = bomIdsBeanX.getProduct_specs();
            department.process_id = bomIdsBeanX.getProcess_id();
            department.qty = bomIdsBeanX.getQty();
            if (bomIdsBeanX.getBom_ids().size()>0) {
                department.setExpanded(false);
                List<BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX.BomIdsBean> employeeList = new ArrayList<>();
                for (int j = 0; j < bomIdsBeanX.getBom_ids().size(); j++) {
                    BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX.BomIdsBean employee = new BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX.BomIdsBean();
                    BomFramworkBean.ResultBean.ResDataBean.BomIdsBeanX.BomIdsBean bomIdsBean = bomIdsBeanX.getBom_ids().get(j);
                    employee.name = bomIdsBean.getName();
                    employee.code = bomIdsBean.getCode();
                    employee.product_specs = bomIdsBean.getProduct_specs();
                    employee.process_id = bomIdsBean.getProcess_id();
                    employee.qty = bomIdsBean.getQty();
                    if (bomIdsBean.getBom_ids().size()>0){
                        employee.setExpanded(false);
                        List<BomSubBean> lastList = new ArrayList<>();
                        for (int k = 0; k < bomIdsBean.getBom_ids().size(); k++){
                            BomSubBean last = new BomSubBean();
                            BomSubBean bomSubBean = bomIdsBean.getBom_ids().get(k);
                            last.name = bomSubBean.getName();
                            last.code = bomSubBean.getCode();
                            last.product_specs = bomSubBean.getProduct_specs();
                            last.process_id = bomSubBean.getProcess_id();
                            last.qty = bomSubBean.getQty();
                            if (bomSubBean.getBom_ids().size()>0){
                                last.setExpanded(false);
                                List<BomSubBean.BomBottomBean> bottomBeanList = new ArrayList<>();
                                for (int l = 0; l < bomSubBean.getBom_ids().size(); l++) {
                                    BomSubBean.BomBottomBean bottomBean = new BomSubBean.BomBottomBean();
                                    BomSubBean.BomBottomBean bottomBean1 = bomSubBean.getBom_ids().get(l);
                                    bottomBean.name = bottomBean1.getName();
                                    bottomBean.code = bottomBean1.getCode();
                                    bottomBean.product_specs = bottomBean1.getProduct_specs();
                                    bottomBean.process_id = bottomBean1.getProcess_id();
                                    bottomBean.qty = bottomBean1.getQty();
                                    if (bottomBean1.getBom_ids().size()>0){
                                        bottomBean.setExpanded(false);
                                        List<BomSubBean.BomBottomBean.SixBomBottomBean> sixBomBottomBeen = new ArrayList<>();
                                        for (int m = 0; m < bottomBean1.getBom_ids().size(); m++) {
                                            BomSubBean.BomBottomBean.SixBomBottomBean bean = new BomSubBean.BomBottomBean.SixBomBottomBean();
                                            BomSubBean.BomBottomBean.SixBomBottomBean bean1 = bottomBean1.getBom_ids().get(m);
                                            bean.name = bean1.getName();
                                            bean.code = bean1.getCode();
                                            bean.product_specs = bean1.getProduct_specs();
                                            bean.process_id = bean1.getProcess_id();
                                            bean.qty = bean1.getQty();
                                            if (bean1.getBom_ids().size()>0){
                                                bean.setExpanded(false);
                                                List<SevenBomBean> sevenBomBeen = new ArrayList<>();
                                                for (int n = 0; n < bean1.getBom_ids().size(); n++) {
                                                    SevenBomBean sevenBom = new SevenBomBean();
                                                    SevenBomBean sevenBom1 = new SevenBomBean();
                                                    sevenBom.name = sevenBom1.getName();
                                                    sevenBom.code = sevenBom1.getCode();
                                                    sevenBom.product_specs = sevenBom1.getProduct_specs();
                                                    sevenBom.process_id = sevenBom1.getProcess_id();
                                                    sevenBom.qty = sevenBom1.getQty();
                                                    sevenBomBeen.add(sevenBom);
                                                }
                                                bean.bom_ids = sevenBomBeen;
                                            }
                                            sixBomBottomBeen.add(bean);
                                        }
                                        bottomBean.bom_ids = sixBomBottomBeen;
                                    }
                                    bottomBeanList.add(bottomBean);
                                }
                                last.bom_ids = bottomBeanList;
                            }
                            lastList.add(last);
                        }
                        employee.bom_ids = lastList;
                    }
                    employeeList.add(employee);
                }
                department.bom_ids = employeeList;
            }
            departments.add(department);
        }
        firstCompany.bom_ids = departments;
        firstCompany.mExpanded = isExpandDefault;
        return firstCompany;
    }
}
