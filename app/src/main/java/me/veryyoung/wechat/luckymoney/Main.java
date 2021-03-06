package me.veryyoung.wechat.luckymoney;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.widget.Button;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType;
import static de.robv.android.xposed.XposedHelpers.newInstance;


public class Main implements IXposedHookLoadPackage {

    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final String LUCKY_MONEY_RECEIVE_UI_CLASS_NAME = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String NOTIFICATION_CLASS_NAME = "com.tencent.mm.booter.notification.b";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) {
        if (lpparam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            findAndHookMethod(NOTIFICATION_CLASS_NAME, lpparam.classLoader, "a", NOTIFICATION_CLASS_NAME, String.class, String.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!PreferencesUtils.open()) {
                                return;
                            }
                            String msgtype = "436207665";
                            if (param.args[3].toString().equals(msgtype)) {
                                String xmlmsg = param.args[2].toString();
                                String xl = xmlmsg.substring(xmlmsg.indexOf("<msg>"));
                                //nativeurl
                                String p = "nativeurl";
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                factory.setNamespaceAware(true);
                                XmlPullParser pz = factory.newPullParser();
                                pz.setInput(new StringReader(xl));
                                int v = pz.getEventType();
                                String saveurl = "";
                                while (v != XmlPullParser.END_DOCUMENT) {
                                    if (v == XmlPullParser.START_TAG) {
                                        if (pz.getName().equals(p)) {
                                            pz.nextToken();
                                            saveurl = pz.getText();
                                            break;
                                        }
                                    }
                                    v = pz.next();
                                }
                                String nativeurl = saveurl;
                                Uri nativeUrl = Uri.parse(nativeurl);
                                int msgType = Integer.parseInt(nativeUrl.getQueryParameter("msgtype"));
                                int channelId = Integer.parseInt(nativeUrl.getQueryParameter("channelid"));
                                String sendId = nativeUrl.getQueryParameter("sendid");
                                String headImg = "";
                                String nickName = "";
                                String sessionUserName = param.args[1].toString();
                                String ver = "v1.0";
                                final Object ab = newInstance(findClass("com.tencent.mm.plugin.luckymoney.c.ab", lpparam.classLoader),
                                        msgType, channelId, sendId, nativeurl, headImg, nickName, sessionUserName, ver);

                                Context context = (Context) callStaticMethod(findClass("com.tencent.mm.sdk.platformtools.aa", lpparam.classLoader), "getContext");
                                final Object i = newInstance(findClass("com.tencent.mm.plugin.luckymoney.c.i", lpparam.classLoader), context, null);

                                if (PreferencesUtils.delay()) {
                                    Thread.sleep(PreferencesUtils.delayTime());
                                }
                                callMethod(i, "a", ab, false);
                            }
                        }
                    }

            );


            findAndHookMethod(LUCKY_MONEY_RECEIVE_UI_CLASS_NAME, lpparam.classLoader, "d", int.class, int.class, String.class, "com.tencent.mm.t.j", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Button button = (Button) findFirstFieldByExactType(param.thisObject.getClass(), Button.class).get(param.thisObject);
                    log(button.getText().toString());
                    if (button.isShown() && button.isClickable()) {
                        button.performClick();
                        callMethod(param.thisObject, "finish");
                    }
                }
            });
            hideModule(lpparam);

        }
    }

    private void hideModule(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<ApplicationInfo> applicationList = (List) param.getResult();
                List<ApplicationInfo> resultapplicationList = new ArrayList<>();
                for (ApplicationInfo applicationInfo : applicationList) {
                    if (!applicationInfo.processName.contains("veryyoung")) {
                        resultapplicationList.add(applicationInfo);
                    }
                }
                param.setResult(resultapplicationList);
            }
        });
        findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<PackageInfo> packageInfoList = (List) param.getResult();
                List<PackageInfo> resultpackageInfoList = new ArrayList<>();
                for (PackageInfo packageInfo : packageInfoList) {
                    if (!packageInfo.packageName.contains("veryyoung")) {
                        resultpackageInfoList.add(packageInfo);
                    }
                }
                param.setResult(resultpackageInfoList);
            }
        });
    }
}
