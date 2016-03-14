#ifndef __HELLOWORLD_SCENE_H__
#define __HELLOWORLD_SCENE_H__

#include "cocos2d.h"
#include "PluginBee7/PluginBee7.h"

class HelloWorld : public cocos2d::Layer, public sdkbox::Bee7Listener
{
public:
    // there's no 'id' in cpp, so we recommend returning the class instance pointer
    static cocos2d::Scene* createScene();

    // Here's a difference. Method 'init' in cocos2d-x returns bool, instead of returning 'id' in cocos2d-iphone
    virtual bool init();

    // implement the "static create()" method manually
    CREATE_FUNC(HelloWorld);

private:
    void createTestMenu();
    
    cocos2d::Label *_pointsLabel;
    cocos2d::Label *_currencyLabel;

    virtual void onAvailableChange(bool available);
    virtual void onVisibleChange(bool available);
    virtual void onGameWallWillClose();
    virtual void onGiveReward(long bee7Points,
                      long virtualCurrencyAmount,
                      const std::string& appId,
                      bool cappedReward,
                      long campaignId,
                      bool videoReward);
};

#endif // __HELLOWORLD_SCENE_H__
