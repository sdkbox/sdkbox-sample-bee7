#ifndef __PluginBee7JS_h__
#define __PluginBee7JS_h__

#include "jsapi.h"
#include "jsfriendapi.h"


#if MOZJS_MAJOR_VERSION >= 33
void js_register_PluginBee7JS_PluginBee7(JSContext *cx, JS::HandleObject global);
void register_all_PluginBee7JS(JSContext* cx, JS::HandleObject obj);
#else
void js_register_PluginBee7JS_PluginBee7(JSContext *cx, JSObject* global);
void register_all_PluginBee7JS(JSContext* cx, JSObject* obj);
#endif
#endif

