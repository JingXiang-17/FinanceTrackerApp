package com.luminous.financetracker.util;

public class Constants {

    // --- DATABASE & APP CONFIG ---
    public static final String DATABASE_NAME = "finance_tracker";

    // Intent Actions or Broadcast Keys (if passing data between your service and UI)
    public static final String ACTION_NEW_TRANSACTION = "com.luminous.financetracker.NEW_TRANSACTION";

    // Shared Preferences Keys (for app settings, dark mode, or onboarding flags)
    public static final String PREF_NAME = "finance_tracker_prefs";
    public static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    // --- TRANSACTION CATEGORIES ---
    public static final String CATEGORY_FIXED = "Fixed";
    public static final String CATEGORY_DINING = "Dining";
    public static final String CATEGORY_TRANSPORT = "Transport";
    public static final String CATEGORY_ENTERTAINMENT = "Entertainment";
    public static final String CATEGORY_SHOPPING = "Shopping";
    public static final String CATEGORY_OTHERS = "Others";

    // --- APP PACKAGE NAMES (NOTIFICATION TARGETS) ---
    // eWallets & Shopping
    public static final String PKG_TNG = "my.com.tngdigital.ewallet";
    public static final String PKG_SHOPEE = "com.shopee.my";
    public static final String PKG_LAZADA = "com.lazada.android";
    public static final String PKG_GRAB = "com.grabtaxi.passenger";
    public static final String PKG_TEMU = "com.einnovation.temu";
    public static final String PKG_WECHAT = "com.tencent.mm";
    public static final String PKG_ALIPAY = "com.eg.android.AlipayGphone";
    public static final String PKG_BOOST = "my.com.myboost";
    // Banks
    public static final String PKG_HLB = "my.com.hongleongconnect.mobileconnect";
    public static final String PKG_RHB = "com.rhbgroup.rhbmobilebanking";
    public static final String PKG_BANK_RAKYAT = "com.irakyatmob.bkrm";
    public static final String PKG_MAE = "com.maybank2u.life";
    public static final String PKG_RYT = "my.rytbank.app";
    public static final String PKG_CIMB = "com.cimbocto";
    public static final String PKG_OCBC = "com.ocbc.mobilemy";

    // --- PAYMENT METHOD STRINGS ---
    public static final String PAY_TNG = "Touch 'n Go eWallet";
    public static final String PAY_SHOPEE = "ShopeePay";
    public static final String PAY_LAZADA = "Lazada Wallet";
    public static final String PAY_GRAB = "GrabPay";
    public static final String PAY_TEMU = "Temu";
    public static final String PAY_WECHAT = "WeChat Pay";
    public static final String PAY_ALIPAY = "Alipay";
    public static final String PAY_BOOST = "Boost";
    public static final String PAY_HLB = "Hong Leong Bank";
    public static final String PAY_RHB = "RHB Bank";
    public static final String PAY_BANK_RAKYAT = "Bank Rakyat";
    public static final String PAY_MAE = "MAE";
    public static final String PAY_RYT = "Ryt Bank";
    public static final String PAY_CIMB = "CIMB Bank";
    public static final String PAY_OCBC = "OCBC Bank";
    public static final String PAY_UNKNOWN = "Unknown App";
    // Other constants
    public static final String KEY_LIMIT_0 = "limit_0", KEY_LIMIT_1 = "limit_1";
    public static final String CHANNEL_BUDGET_ALERTS = "budget_alerts";
    public static final String PERIOD_DAILY = "Daily";
    public static final String PERIOD_MONTHLY = "Monthly";
    public static final String KEY_PROFILE_PIC_PATH = "profile_pic_path";
    public static final String KEY_AUTO_CALC_BUDGETS = "auto_calc_budgets";

    // Private constructor to prevent instantiation
    private Constants() {}
}