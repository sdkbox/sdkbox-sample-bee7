#ifndef __PluginBee7JS_h__
#define __PluginBee7JS_h__

#include "jsapi.h"
#include "jsfriendapi.h"


extern JSClass  *jsb_sdkbox_PluginBee7_class;
extern JSObject *jsb_sdkbox_PluginBee7_prototype;

#if MOZJS_MAJOR_VERSION >= 33
void js_register_PluginBee7JS_PluginBee7(JSContext *cx, JS::HandleObject global);
void register_all_PluginBee7JS(JSContext* cx, JS::HandleObject obj);
#else
void js_register_PluginBee7JS_PluginBee7(JSContext *cx, JSObject* global);
void register_all_PluginBee7JS(JSContext* cx, JSObject* obj);
#endif

bool js_PluginBee7JS_PluginBee7_constructor(JSContext *cx, uint32_t argc, jsval *vp);
void js_PluginBee7JS_PluginBee7_finalize(JSContext *cx, JSObject *obj);
#if defined(MOZJS_MAJOR_VERSION)
bool js_PluginBee7JS_PluginBee7_showGameWall(JSContext *cx, uint32_t argc, jsval *vp);
#elif defined(JS_VERSION)
JSBool js_PluginBee7JS_PluginBee7_showGameWall(JSContext *cx, uint32_t argc, jsval *vp);
#endif
#if defined(MOZJS_MAJOR_VERSION)
bool js_PluginBee7JS_PluginBee7_init(JSContext *cx, uint32_t argc, jsval *vp);
#elif defined(JS_VERSION)
JSBool js_PluginBee7JS_PluginBee7_init(JSContext *cx, uint32_t argc, jsval *vp);
#endif
#endif

