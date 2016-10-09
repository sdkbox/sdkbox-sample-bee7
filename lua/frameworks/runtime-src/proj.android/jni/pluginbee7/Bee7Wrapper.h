//
//  Bee7Wrapper.h
//  PluginBee7
//
//  Copyright (c) 2015 chukong. All rights reserved.
//


#ifndef __PLUGIN_BEE7_WRAPPER_H__
#define __PLUGIN_BEE7_WRAPPER_H__

#include "CocosMacros.h"
#include "PluginBee7.h"

NS_COCOS_BEGIN

#define TAG        "Bee7"
#define VERSION    "2.11.6"
#define ANDROID_VERSION "android 2.3.9"
#define IOS_VERSION     "ios 2.11.6"

class Bee7Wrapper
{
public:
    Bee7Wrapper();
    static Bee7Wrapper* getInstance();

    virtual void init() = 0;
    virtual void __init( bool asAdUnit ) = 0;
    virtual void setListener(Bee7Listener* listener) = 0;
    virtual Bee7Listener* getListener() = 0;
    virtual void removeListener() = 0;
    virtual void showGameWall() = 0;
private:
    static Bee7Wrapper* _instance;
};

class Bee7WrapperDisabled : public Bee7Wrapper
{
public:
    Bee7WrapperDisabled() {}
    virtual ~Bee7WrapperDisabled() {}
    void init() {}
    void __init( bool asAdUnit ) {}
    void setListener(Bee7Listener* listener) {}
    Bee7Listener* getListener() { return nullptr; }
    void removeListener() {}
    void showGameWall() {}
};

class Bee7WrapperEnabled : public Bee7Wrapper
{
public:
    Bee7WrapperEnabled();
    ~Bee7WrapperEnabled();

    void init();
    void __init( bool asAdUnit );

    void setListener(Bee7Listener* listener);
    Bee7Listener* getListener();
    void removeListener();
    void showGameWall();

protected:
    bool nativeInit(const std::string& key, bool asAdUnit );
    std::string nativeSDKVersion() const;

    Bee7Listener* _listener;

private:
    bool _initialized;
};

NS_COCOS_END

#endif
