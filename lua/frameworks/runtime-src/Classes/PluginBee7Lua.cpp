#include "PluginBee7Lua.hpp"
#include "PluginBee7/PluginBee7.h"
#include "SDKBoxLuaHelper.h"
#include "sdkbox/Sdkbox.h"



int lua_PluginBee7Lua_PluginBee7_showGameWall(lua_State* tolua_S)
{
    int argc = 0;
    bool ok  = true;

#if COCOS2D_DEBUG >= 1
    tolua_Error tolua_err;
#endif

#if COCOS2D_DEBUG >= 1
    if (!tolua_isusertable(tolua_S,1,"sdkbox.PluginBee7",0,&tolua_err)) goto tolua_lerror;
#endif

    argc = lua_gettop(tolua_S) - 1;

    if (argc == 0)
    {
        if(!ok)
        {
            tolua_error(tolua_S,"invalid arguments in function 'lua_PluginBee7Lua_PluginBee7_showGameWall'", nullptr);
            return 0;
        }
        sdkbox::PluginBee7::showGameWall();
        lua_settop(tolua_S, 1);
        return 1;
    }
    luaL_error(tolua_S, "%s has wrong number of arguments: %d, was expecting %d\n ", "sdkbox.PluginBee7:showGameWall",argc, 0);
    return 0;
#if COCOS2D_DEBUG >= 1
    tolua_lerror:
    tolua_error(tolua_S,"#ferror in function 'lua_PluginBee7Lua_PluginBee7_showGameWall'.",&tolua_err);
#endif
    return 0;
}
int lua_PluginBee7Lua_PluginBee7_init(lua_State* tolua_S)
{
    int argc = 0;
    bool ok  = true;

#if COCOS2D_DEBUG >= 1
    tolua_Error tolua_err;
#endif

#if COCOS2D_DEBUG >= 1
    if (!tolua_isusertable(tolua_S,1,"sdkbox.PluginBee7",0,&tolua_err)) goto tolua_lerror;
#endif

    argc = lua_gettop(tolua_S) - 1;

    if (argc == 0)
    {
        if(!ok)
        {
            tolua_error(tolua_S,"invalid arguments in function 'lua_PluginBee7Lua_PluginBee7_init'", nullptr);
            return 0;
        }
        sdkbox::PluginBee7::init();
        lua_settop(tolua_S, 1);
        return 1;
    }
    luaL_error(tolua_S, "%s has wrong number of arguments: %d, was expecting %d\n ", "sdkbox.PluginBee7:init",argc, 0);
    return 0;
#if COCOS2D_DEBUG >= 1
    tolua_lerror:
    tolua_error(tolua_S,"#ferror in function 'lua_PluginBee7Lua_PluginBee7_init'.",&tolua_err);
#endif
    return 0;
}
static int lua_PluginBee7Lua_PluginBee7_finalize(lua_State* tolua_S)
{
    printf("luabindings: finalizing LUA object (PluginBee7)");
    return 0;
}

int lua_register_PluginBee7Lua_PluginBee7(lua_State* tolua_S)
{
    tolua_usertype(tolua_S,"sdkbox.PluginBee7");
    tolua_cclass(tolua_S,"PluginBee7","sdkbox.PluginBee7","",nullptr);

    tolua_beginmodule(tolua_S,"PluginBee7");
        tolua_function(tolua_S,"showGameWall", lua_PluginBee7Lua_PluginBee7_showGameWall);
        tolua_function(tolua_S,"init", lua_PluginBee7Lua_PluginBee7_init);
    tolua_endmodule(tolua_S);
    std::string typeName = typeid(sdkbox::PluginBee7).name();
    g_luaType[typeName] = "sdkbox.PluginBee7";
    g_typeCast["PluginBee7"] = "sdkbox.PluginBee7";
    return 1;
}
TOLUA_API int register_all_PluginBee7Lua(lua_State* tolua_S)
{
	tolua_open(tolua_S);
	
	tolua_module(tolua_S,"sdkbox",0);
	tolua_beginmodule(tolua_S,"sdkbox");

	lua_register_PluginBee7Lua_PluginBee7(tolua_S);

	tolua_endmodule(tolua_S);

	sdkbox::setProjectType("lua");
	return 1;
}

