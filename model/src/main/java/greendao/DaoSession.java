package greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import tarce.model.greendaoBean.LoginResponseBean;
import tarce.model.greendaoBean.MenuListBean;
import tarce.model.greendaoBean.UserLogin;
import tarce.model.greendaoBean.ContactsBean;

import greendao.LoginResponseBeanDao;
import greendao.MenuListBeanDao;
import greendao.UserLoginDao;
import greendao.ContactsBeanDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig loginResponseBeanDaoConfig;
    private final DaoConfig menuListBeanDaoConfig;
    private final DaoConfig userLoginDaoConfig;
    private final DaoConfig contactsBeanDaoConfig;

    private final LoginResponseBeanDao loginResponseBeanDao;
    private final MenuListBeanDao menuListBeanDao;
    private final UserLoginDao userLoginDao;
    private final ContactsBeanDao contactsBeanDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        loginResponseBeanDaoConfig = daoConfigMap.get(LoginResponseBeanDao.class).clone();
        loginResponseBeanDaoConfig.initIdentityScope(type);

        menuListBeanDaoConfig = daoConfigMap.get(MenuListBeanDao.class).clone();
        menuListBeanDaoConfig.initIdentityScope(type);

        userLoginDaoConfig = daoConfigMap.get(UserLoginDao.class).clone();
        userLoginDaoConfig.initIdentityScope(type);

        contactsBeanDaoConfig = daoConfigMap.get(ContactsBeanDao.class).clone();
        contactsBeanDaoConfig.initIdentityScope(type);

        loginResponseBeanDao = new LoginResponseBeanDao(loginResponseBeanDaoConfig, this);
        menuListBeanDao = new MenuListBeanDao(menuListBeanDaoConfig, this);
        userLoginDao = new UserLoginDao(userLoginDaoConfig, this);
        contactsBeanDao = new ContactsBeanDao(contactsBeanDaoConfig, this);

        registerDao(LoginResponseBean.class, loginResponseBeanDao);
        registerDao(MenuListBean.class, menuListBeanDao);
        registerDao(UserLogin.class, userLoginDao);
        registerDao(ContactsBean.class, contactsBeanDao);
    }
    
    public void clear() {
        loginResponseBeanDaoConfig.clearIdentityScope();
        menuListBeanDaoConfig.clearIdentityScope();
        userLoginDaoConfig.clearIdentityScope();
        contactsBeanDaoConfig.clearIdentityScope();
    }

    public LoginResponseBeanDao getLoginResponseBeanDao() {
        return loginResponseBeanDao;
    }

    public MenuListBeanDao getMenuListBeanDao() {
        return menuListBeanDao;
    }

    public UserLoginDao getUserLoginDao() {
        return userLoginDao;
    }

    public ContactsBeanDao getContactsBeanDao() {
        return contactsBeanDao;
    }

}
