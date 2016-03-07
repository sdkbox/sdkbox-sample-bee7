
#include "PluginBee7LuaHelper.h"
#include "PluginBee7/PluginBee7.h"
#include "CCLuaEngine.h"
#include "tolua_fix.h"
#include "SDKBoxLuaHelper.h"

class Bee7ListenerLua : public sdkbox::Bee7Listener
{
public:
    Bee7ListenerLua(): mLuaHandler(0)
    {
    }
    ~Bee7ListenerLua()
    {
        resetHandler();
    }
    void setHandler(int luaHandler)
    {
        if (mLuaHandler == luaHandler)
        {
            return;
        }
        resetHandler();
        mLuaHandler = luaHandler;
    }
    void resetHandler()
    {
        if (!mLuaHandler)
        {
            return;
        }

        LUAENGINE->removeScriptHandler(mLuaHandler);
        mLuaHandler = 0;
    }

    void onAvailableChange(bool available)
    {
        LuaStack* stack = LUAENGINE->getLuaStack();

        LuaValueDict dict;
        dict.insert(std::make_pair("name", LuaValue::stringValue("onAvailableChange")));
        dict.insert(std::make_pair("available", LuaValue::booleanValue(available)));
        stack->pushLuaValueDict(dict);
        stack->executeFunctionByHandler(mLuaHandler, 1);
    }
    void onVisibleChange(bool available)
    {
        LuaStack* stack = LUAENGINE->getLuaStack();

        LuaValueDict dict;
        dict.insert(std::make_pair("name", LuaValue::stringValue("onVisibleChange")));
        dict.insert(std::make_pair("available", LuaValue::booleanValue(available)));
        stack->pushLuaValueDict(dict);
        stack->executeFunctionByHandler(mLuaHandler, 1);
    }
    void onGameWallWillClose()
    {
        LuaStack* stack = LUAENGINE->getLuaStack();

        LuaValueDict dict;
        dict.insert(std::make_pair("name", LuaValue::stringValue("onGameWallWillClose")));
        stack->pushLuaValueDict(dict);
        stack->executeFunctionByHandler(mLuaHandler, 1);
    }
    void onGiveReward(long bee7Points,
                      long virtualCurrencyAmount,
                      const std::string& appId,
                      bool cappedReward,
                      long campaignId,
                      bool videoReward)
    {
        LuaStack* stack = LUAENGINE->getLuaStack();

        LuaValueDict dict;
        dict.insert(std::make_pair("name", LuaValue::stringValue("onGiveReward")));
        dict.insert(std::make_pair("points", LuaValue::intValue(bee7Points)));
        dict.insert(std::make_pair("amount", LuaValue::intValue(virtualCurrencyAmount)));
        dict.insert(std::make_pair("appId", LuaValue::stringValue(appId)));
        dict.insert(std::make_pair("cappedReward", LuaValue::booleanValue(cappedReward)));
        dict.insert(std::make_pair("campaignId", LuaValue::intValue(campaignId)));
        dict.insert(std::make_pair("videoReward", LuaValue::booleanValue(videoReward)));

        stack->pushLuaValueDict(dict);
        stack->executeFunctionByHandler(mLuaHandler, 1);
    }
private:
    int mLuaHandler;
};

int lua_PluginBee7Lua_PluginBee7_setListener(lua_State* tolua_S) {
    int argc = 0;

#if COCOS2D_DEBUG >= 1
    tolua_Error tolua_err;
#endif

#if COCOS2D_DEBUG >= 1
    if (!tolua_isusertable(tolua_S,1,"sdkbox.PluginBee7",0,&tolua_err)) goto tolua_lerror;
#endif

    argc = lua_gettop(tolua_S) - 1;

    if (argc == 1)
    {
#if COCOS2D_DEBUG >= 1
        if (!toluafix_isfunction(tolua_S, 2 , "LUA_FUNCTION",0,&tolua_err))
        {
            goto tolua_lerror;
        }
#endif
        LUA_FUNCTION handler = (  toluafix_ref_function(tolua_S,2,0));
        Bee7ListenerLua* lis = static_cast<Bee7ListenerLua*> (sdkbox::PluginBee7::getListener());
        if (nullptr == lis) {
            lis = new Bee7ListenerLua();
        }
        lis->setHandler(handler);
        sdkbox::PluginBee7::setListener(lis);

        return 0;
    }
    luaL_error(tolua_S, "%s has wrong number of arguments: %d, was expecting %d\n ", "sdkbox.PluginBee7::setListener",argc, 1);
    return 0;
#if COCOS2D_DEBUG >= 1
tolua_lerror:
    tolua_error(tolua_S,"#ferror in function 'lua_PluginBee7Lua_PluginBee7_setListener'.",&tolua_err);
#endif
    return 0;
}

int extern_PluginBee7(lua_State* L) {
    if (nullptr == L) {
        return 0;
    }

    lua_pushstring(L, "sdkbox.PluginBee7");
    lua_rawget(L, LUA_REGISTRYINDEX);
    if (lua_istable(L,-1))
    {
        tolua_function(L,"setListener", lua_PluginBee7Lua_PluginBee7_setListener);
    }
    lua_pop(L, 1);

    return 1;
}

TOLUA_API int register_all_PluginBee7Lua_helper(lua_State* L) {
    tolua_module(L,"sdkbox",0);
    tolua_beginmodule(L,"sdkbox");

    extern_PluginBee7(L);

    tolua_endmodule(L);
    return 1;
}


