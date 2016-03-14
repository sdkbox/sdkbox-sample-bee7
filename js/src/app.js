
var HelloWorldLayer = cc.Layer.extend({
    sprite:null,
    ctor:function () {
        //////////////////////////////
        // 1. super init first
        this._super();

        cc.log("Sample Startup")

        this.createTestMenu();

        var winsize = cc.winSize;

        var logo = new cc.Sprite("res/Logo.png");
        var logoSize = logo.getContentSize();
        logo.x = logoSize.width / 2;
        logo.y = winsize.height - logoSize.height / 2;
        this.addChild(logo);

        var quit = new cc.MenuItemLabel(new cc.LabelTTF("QUIT", "sans", 32), function() {
            cc.log("QUIT");
        });
        var menu = new cc.Menu(quit);
        var size = quit.getContentSize();
        menu.x = winsize.width - size.width / 2 - 16;
        menu.y = size.height / 2 + 16;
        this.addChild(menu);

        return true;
    },

    createTestMenu:function() {
        var pointsLabel = cc.LabelTTF.create("0 points", "sans", 32);
        pointsLabel.x = cc.winSize.width / 2;
        pointsLabel.y = 100;
        this.addChild(pointsLabel);

        var currencyLabel = cc.LabelTTF.create("0 currency", "sans", 32);
        currencyLabel.x = cc.winSize.width / 2;
        currencyLabel.y = 140;
        this.addChild(currencyLabel);

        sdkbox.PluginBee7.init();
        sdkbox.PluginBee7.setListener({
            onAvailableChange: function(available) {
                cc.log("onAvailableChange: " + available.toString());
            },
            onVisibleChange: function(available) {
                cc.log("onVisibleChange: " + available.toString());
            },
            onGameWallWillClose: function() {
                cc.log("onGameWallWillClose");
            },
            onGiveReward: function(bee7Points, virtualCurrencyAmount, appId, cappedReward,
                                   campaignId, videoReward) {
                cc.log("onGiveReward");
                cc.log("bee7Points: " + bee7Points.toString());
                cc.log("virtualCurrencyAmount: " + virtualCurrencyAmount.toString());
                cc.log("appId: " + appId.toString());
                cc.log("cappedReward: " + cappedReward.toString());
                cc.log("campaignId: " + campaignId.toString());
                cc.log("videoReward: " + videoReward.toString());

                pointsLabel.setString(bee7Points.toString() + " points");
                currencyLabel.setString(virtualCurrencyAmount.toString() + " currency");
            }
        });

        var item1 = new cc.MenuItemLabel(new cc.LabelTTF("show game wall", "sans", 28), function() {
            cc.log("show game wall");
            sdkbox.PluginBee7.showGameWall();
        });

        var winsize = cc.winSize;
        var menu = new cc.Menu(item1);
        menu.x = winsize.width / 2;
        menu.y = winsize.height / 2;
        menu.alignItemsVerticallyWithPadding(20);
        this.addChild(menu);
    }
});

var HelloWorldScene = cc.Scene.extend({
    onEnter:function () {
        this._super();
        var layer = new HelloWorldLayer();
        this.addChild(layer);
    }
});

