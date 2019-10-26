#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(MapboxNative, RCTViewManager)
    RCT_EXTERN_METHOD(setCoordinates: (nonnull NSArray *)coordinateList)
    RCT_EXTERN_METHOD(drawPolygon: (nonnull NSArray *)coordinateList)
    RCT_EXTERN_METHOD(startNavigation)
    RCT_EXTERN_METHOD(stopNavigation)
    RCT_EXTERN_METHOD(clearMapItems)
    RCT_EXTERN_METHOD(addPoint: (nonnull NSArray *)coordinates setCamera:(nonnull NSNumber *)camera callback:(RCTResponseSenderBlock *)successCallback)
@end
