/****************************************************************************

 Copyright (c) 2014-2016 SDKBOX Inc

 ****************************************************************************/
#ifndef _PLUGIN_BEE7_H_
#define _PLUGIN_BEE7_H_

#include <string>

namespace sdkbox
{
    class Bee7Listener
    {
    public:
        virtual void onAvailableChange(bool available) = 0;
        virtual void onVisibleChange(bool available) = 0;
        virtual void onGameWallWillClose() = 0;
        virtual void onGiveReward(long bee7Points,
                                  long virtualCurrencyAmount,
                                  const std::string& appId,
                                  bool cappedReward,
                                  long campaignId,
                                  bool videoReward) = 0;

    };

    class PluginBee7
    {
    public:
        /*!
         * initialize the plugin instance.
         */
        static void init();

        /**
         * Set listener to listen for bee7 events
         */
        static void setListener(Bee7Listener* listener);

        /**
         * Get the listener
         */
        static Bee7Listener* getListener();

        /**
         * Remove the listener, and can't listen to events anymore
         */
        static void removeListener();

        static void showGameWall();
    };
}

#endif
