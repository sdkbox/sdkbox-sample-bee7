
#include "HelloWorldScene.h"
#include "PluginBee7/PluginBee7.h"

USING_NS_CC;

Scene* HelloWorld::createScene()
{
    // 'scene' is an autorelease object
    auto scene = Scene::create();

    // 'layer' is an autorelease object
    auto layer = HelloWorld::create();

    // add layer as a child to scene
    scene->addChild(layer);

    // return the scene
    return scene;
}

// on "init" you need to initialize your instance
bool HelloWorld::init()
{
    //////////////////////////////
    // 1. super init first
    if ( !Layer::init() )
    {
        return false;
    }

    CCLOG("Sample Startup");

    // add logo
    auto winsize = Director::getInstance()->getWinSize();
    auto logo = Sprite::create("Logo.png");
    auto logoSize = logo->getContentSize();
    logo->setPosition(Vec2(logoSize.width / 2,
                           winsize.height - logoSize.height / 2));
    addChild(logo);

    // add quit button
    auto label = Label::createWithSystemFont("QUIT", "sans", 32);
    auto quit = MenuItemLabel::create(label, [](Ref*){
        exit(0);
    });
    auto labelSize = label->getContentSize();
    quit->setPosition(Vec2(winsize.width / 2 - labelSize.width / 2 - 16,
                           -winsize.height / 2 + labelSize.height / 2 + 16));
    addChild(Menu::create(quit, NULL));

    // add test menu
    createTestMenu();

    return true;
}

void HelloWorld::createTestMenu()
{
    Size size = Director::getInstance()->getVisibleSize();
    auto menu = Menu::create();

    sdkbox::PluginBee7::init();
    sdkbox::PluginBee7::setListener(this);
    
    _pointsLabel = Label::createWithSystemFont("0", "sans", 32);
    _pointsLabel->setPosition(Vec2(size.width / 2 - 100, 100));
    addChild(_pointsLabel);

    auto label = Label::createWithSystemFont("points", "sans", 32);
    label->setPosition(Vec2(size.width / 2, 100));
    addChild(label);

    _currencyLabel = Label::createWithSystemFont("0", "sans", 32);
    _currencyLabel->setPosition(Vec2(size.width / 2 - 100, 140));
    addChild(_currencyLabel);

    label = Label::createWithSystemFont("currency", "sans", 32);
    label->setPosition(Vec2(size.width / 2, 140));
    addChild(label);

    menu->addChild(MenuItemLabel::create(Label::createWithSystemFont("show game wall", "sans", 24), [](Ref*){
        CCLOG("show game wall");
        sdkbox::PluginBee7::showGameWall();
    }));

    menu->alignItemsVerticallyWithPadding(10);
    addChild(menu);
}

void HelloWorld::onAvailableChange(bool available)
{
    CCLOG("onAvailableChange: %s", available ? "YES" : "NO");
}

void HelloWorld::onVisibleChange(bool available)
{
    CCLOG("onVisibleChange: %s", available ? "YES" : "NO");
}

void HelloWorld::onGameWallWillClose()
{
    CCLOG("onGameWallWillClose");
}

template <typename T>
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}

void HelloWorld::onGiveReward(long bee7Points,
                              long virtualCurrencyAmount,
                              const std::string& appId,
                              bool cappedReward,
                              long campaignId,
                              bool videoReward)
{
    CCLOG("onGiveReward");
    CCLOG(" - bee7Points: %ld", bee7Points);
    CCLOG(" - virtualCurrencyAmount: %ld", virtualCurrencyAmount);
    CCLOG(" - appId: %s", appId.c_str());
    CCLOG(" - cappedReward: %s", cappedReward ? "YES" : "NO");
    CCLOG(" - campaignId: %ld", campaignId);
    CCLOG(" - videoReward: %s", videoReward ? "YES" : "NO");
    
    _pointsLabel->setString(to_string(bee7Points).c_str());
    _currencyLabel->setString(to_string(virtualCurrencyAmount).c_str());
}
