/**
 * bt-ble-sensors
 *
 * Created by Jason Waring
 * Copyright (c) 2015 SoftWaring Solutions. All rights reserved.
 */

#import "TiModule.h"
#import "DEACentralManager.h"

#define USE_FUNCTIONS_FOR_EVENT 0

@interface ComSwsSensorsBtBleModule : TiModule <CBCentralManagerDelegate, CBPeripheralDelegate>
{
@private
#if USE_FUNCTIONS_FOR_EVENT
    // The JavaScript callbacks (KrollCallback objects)
    KrollCallback *statusCallback;
    KrollCallback *scanningCallback;
    KrollCallback *connectionCallback;
    KrollCallback *dataCallback;
#endif

    NSString *requestedConnection;
}

/* BLE API */
-(id)isAvailable:(id)no_args;
-(id)isEnabled:(id)no_args;
-(void)enable:(id)no_args;
-(void)disable:(id)no_args;

#if USE_FUNCTIONS_FOR_EVENT
-(void)addListeners:(id)listeners;
-(void)removeListeners:(id)listenerNames;
-(void)removeAllListeners:(id)no_args;
#endif

-(void)startDiscovery:(id)no_args;
-(void)cancelDiscovery:(id)no_args;

-(id)isConnected:(id)uuid;
-(void)connect:(NSArray *)args; // NSString *uuid[, NSString *hint]
-(void)disconnect:(id)uuid;
-(void)update:(NSArray *)args;  // NSString *uuid, NSDictionary *values

@end
