
#include "PluginBee7JSHelper.h"
#include "SDKBoxJSHelper.h"
#include "PluginBee7/PluginBee7.h"

static JSContext* s_cx = nullptr;

class Bee7ListenerJsHelper : public sdkbox::Bee7Listener, public sdkbox::JSListenerBase
{
public:
    Bee7ListenerJsHelper():sdkbox::JSListenerBase() {
    }

    void onAvailableChange(bool available)
    {
        std::string name("onAvailableChange");
        JS::Value dataVal[1];
        dataVal[0] = JS::BooleanValue(available);
        invokeDelegate(name, dataVal, 1);
    }
    void onVisibleChange(bool available)
    {
        std::string name("onVisibleChange");
        JS::Value dataVal[1];
        dataVal[0] = JS::BooleanValue(available);
        invokeDelegate(name, dataVal, 1);
    }
    void onGameWallWillClose()
    {
        std::string name("onGameWallWillClose");
        JS::Value dataVal[0];
        invokeDelegate(name, dataVal, 0);
    }
    void onGiveReward(long bee7Points,
                      long virtualCurrencyAmount,
                      const std::string& appId,
                      bool cappedReward,
                      long campaignId,
                      bool videoReward)
    {
        std::string name("onGiveReward");
        JS::Value dataVal[6];
        dataVal[0] = JS::NumberValue(bee7Points);
        dataVal[1] = JS::NumberValue(virtualCurrencyAmount);
        dataVal[2] = SB_STR_TO_JSVAL(s_cx, appId);
        dataVal[3] = JS::BooleanValue(cappedReward);
        dataVal[4] = JS::NumberValue(campaignId);
        dataVal[5] = JS::BooleanValue(videoReward);

        invokeDelegate(name, dataVal, 6);
    }
private:
    void invokeDelegate(std::string& fName, JS::Value dataVal[], int argc) {
        if (!s_cx) {
            return;
        }
        JSContext* cx = s_cx;
        const char* func_name = fName.c_str();

        JS::RootedObject obj(cx, getJSDelegate());
        JSAutoCompartment ac(cx, obj);

#if MOZJS_MAJOR_VERSION >= 31
        bool hasAction;
        JS::RootedValue retval(cx);
        JS::RootedValue func_handle(cx);
#elif MOZJS_MAJOR_VERSION >= 28
        bool hasAction;
        jsval retval;
        JS::RootedValue func_handle(cx);
#else
        JSBool hasAction;
        jsval retval;
        jsval func_handle;
#endif

        if (JS_HasProperty(cx, obj, func_name, &hasAction) && hasAction) {
            if(!JS_GetProperty(cx, obj, func_name, &func_handle)) {
                return;
            }
            if(func_handle == JS::NullValue()) {
                return;
            }

#if MOZJS_MAJOR_VERSION >= 31
            if (0 == argc) {
                JS_CallFunctionName(cx, obj, func_name, JS::HandleValueArray::empty(), &retval);
            } else {
                JS_CallFunctionName(cx, obj, func_name, JS::HandleValueArray::fromMarkedLocation(argc, dataVal), &retval);
            }
#else
            if (0 == argc) {
                JS_CallFunctionName(cx, obj, func_name, 0, nullptr, &retval);
            } else {
                JS_CallFunctionName(cx, obj, func_name, argc, dataVal, &retval);
            }
#endif
        }
    }
};


#if defined(MOZJS_MAJOR_VERSION)
bool js_PluginBee7JS_PluginBee7_setListener(JSContext *cx, uint32_t argc, JS::Value *vp)
#elif defined(JS_VERSION)
JSBool js_PluginBee7JS_PluginBee7_setListener(JSContext *cx, unsigned argc, JS::Value *vp)
#endif
{
    s_cx = cx;
    JS::CallArgs args = JS::CallArgsFromVp(argc, vp);
    bool ok = true;

    if (argc == 1) {

        if (!args.get(0).isObject())
        {
            ok = false;
        }

        JSB_PRECONDITION2(ok, cx, false, "js_PluginBee7JS_PluginBee7_setListener : Error processing arguments");
        Bee7ListenerJsHelper* lis = new Bee7ListenerJsHelper();
        lis->setJSDelegate(cx, args.get(0));
        sdkbox::PluginBee7::setListener(lis);

        args.rval().setUndefined();
        return true;
    }
    JS_ReportErrorUTF8(cx, "js_PluginBee7JS_PluginBee7_setListener : wrong number of arguments");
    return false;
}

#define REGISTE_BEE7_FUNCTIONS \
JS_DefineFunction(cx, pluginObj, "setListener", js_PluginBee7JS_PluginBee7_setListener, 1, JSPROP_READONLY | JSPROP_PERMANENT);

#if defined(MOZJS_MAJOR_VERSION)
#if MOZJS_MAJOR_VERSION >= 33
void register_all_PluginBee7JS_helper(JSContext* cx, JS::HandleObject global) {
    // Get the ns
    JS::RootedObject pluginObj(cx);
    sdkbox::getJsObjOrCreat(cx, global, "sdkbox.PluginBee7", &pluginObj);

    REGISTE_BEE7_FUNCTIONS
}
#else
void register_all_PluginBee7JS_helper(JSContext* cx, JSObject* obj) {
    // first, try to get the ns
    JS::RootedValue nsval(cx);
    JS::RootedObject pluginObj(cx);

    std::stringstream ss("sdkbox.PluginBee7");
    std::string sub;
    const char* subChar;

    while(getline(ss, sub, '.')) {
        if(sub.empty())continue;

        subChar = sub.c_str();

        JS_GetProperty(cx, obj, subChar, &nsval);
        if (nsval == JS::NullValue()) {
            pluginObj = JS_NewObject(cx, NULL, NULL, NULL);
            nsval = OBJECT_TO_JSVAL(pluginObj);
            JS_SetProperty(cx, obj, subChar, nsval);
        } else {
            JS_ValueToObject(cx, nsval, &pluginObj);
        }
        obj = pluginObj;
    }

    REGISTE_BEE7_FUNCTIONS
}
#endif
#elif defined(JS_VERSION)
void register_all_PluginBee7JS_helper(JSContext* cx, JSObject* global) {
    jsval pluginVal;
    JSObject* pluginObj;
    pluginVal = sdkbox::getJsObjOrCreat(cx, global, "sdkbox.PluginBee7", &pluginObj);

    REGISTE_BEE7_FUNCTIONS
}
#endif

