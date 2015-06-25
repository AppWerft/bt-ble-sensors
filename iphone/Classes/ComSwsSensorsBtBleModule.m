/**
 * bt-ble-sensors
 *
 * Created by Jason Waring
 * Copyright (c) 2015 SoftWaring Solutions. All rights reserved.
 */

#import "ComSwsSensorsBtBleModule.h"
#include "DEASensorTag.h"
#include "BLEHeartRate.h"
#include "BLEHeartRateService.h"
#include "BLEMultispread.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

#define STATUS_EVENT @"bluetooth-le:status"
#define SCANNING_EVENT @"bluetooth-le:scanning"
#define CONNECTION_EVENT @"bluetooth-le:connection"
#define DATA_EVENT @"bluetooth-le:data"

@interface ComSwsSensorsBtBleModule ()
-(NSString *)currentTimestamp;
@end

@implementation ComSwsSensorsBtBleModule


#pragma mark Internal

-(id)moduleGUID {
    return @"049d9453-b617-471f-912a-835a21fb0542";
}

-(NSString*)moduleId {
    return @"com.sws.sensors.bt.ble";
}

#pragma mark Lifecycle

-(void)startup {
    [super startup];
    
    NSLog(@"[BLE-SENSORS] %@ started", self);
    
    DEACentralManager *centralManager = [DEACentralManager initSharedServiceWithDelegate:self];
    centralManager.delegate = self;
    
    [centralManager addObserver:self
                     forKeyPath:@"isScanning"
                        options:NSKeyValueObservingOptionNew
                        context:NULL];
}

-(void)shutdown:(id)sender
{
    // this method is called when the module is being unloaded
    // typically this is during shutdown. make sure you don't do too
    // much processing here or the app will be quit forceably
    NSLog(@"[BLE-SENSORS] %@ shutdown", self);
    
    // you *must* call the superclass
    [super shutdown:sender];
}

#pragma mark Cleanup

-(void)dealloc
{
    
#if USE_FUNCTIONS_FOR_EVENT
    // Make sure to release the callback objects
    RELEASE_TO_NIL(statusCallback);
    RELEASE_TO_NIL(scanningCallback);
    RELEASE_TO_NIL(connectionCallback);
    RELEASE_TO_NIL(dataCallback);
#endif
    
    // release any resources that have been retained by the module
    [super dealloc];
}

#pragma mark Internal Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
    // optionally release any resources that can be dynamically
    // reloaded once memory is available - such as caches
    [super didReceiveMemoryWarning:notification];
}

#pragma Public APIs

-(id)isAvailable:(id)no_args {
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    CBCentralManagerState state = [centralManager.manager state];
    
    return NUMBOOL((state == CBCentralManagerStatePoweredOn));
}

-(id)isEnabled:(id)no_args {
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    CBCentralManagerState state = [centralManager.manager state];
    
    return NUMBOOL((state == CBCentralManagerStatePoweredOn));
}

-(void)enable:(id)no_args {
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    
    // Can't control startup of BLE from app.
}

-(void)disable:(id)no_args {
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    
    // Can't control shutdown of BLE from app.
}


#if USE_FUNCTIONS_FOR_EVENT

/* Add listeners by name.
 * listeners - Dictionary of listener; may include status, scanning or data
 */
-(void)addListeners:(id)listeners {
    ENSURE_SINGLE_ARG(listeners, NSDictionary);
    
    NSLog(@"[BLE-SENSORS] Add listeners");
    
    if ([listeners objectForKey:STATUS_EVENT] != nil) {
        statusCallback = [[listeners objectForKey:STATUS_EVENT] retain];
    }
    
    if ([listeners objectForKey:SCANNING_EVENT] != nil) {
        scanningCallback = [[listeners objectForKey:SCANNING_EVENT] retain];
    }
    
    if ([listeners objectForKey:CONNECTION_EVENT] != nil) {
        connectionCallback = [[listeners objectForKey:CONNECTION_EVENT] retain];
    }
    
    if ([listeners objectForKey:DATA_EVENT] != nil) {
        dataCallback = [[listeners objectForKey:DATA_EVENT] retain];
    }
}

/* Remove listeners by name.
 * listenerNames - Array of listener names; may include status, scanning or data
 */
-(void)removeListeners:(id)listenerNames {

    ENSURE_SINGLE_ARG(listenerNames, NSArray);
    
    NSLog(@"[BLE-SENSORS] Remove selected listeners");
    
    for (NSString *key in listenerNames) {
        if ([key isEqualToString:STATUS_EVENT]) {
            RELEASE_TO_NIL(statusCallback);
            
        } else if ([key isEqualToString:SCANNING_EVENT]) {
            RELEASE_TO_NIL(scanningCallback);
            
        } else if ([key isEqualToString:CONNECTION_EVENT]) {
            RELEASE_TO_NIL(connectionCallback);
            
        } else if ([key isEqualToString:DATA_EVENT]) {
            RELEASE_TO_NIL(dataCallback);
        }
    }
}


/* Remove all registered listeners.
 */
-(void)removeAllListeners:(id)no_args {
    NSLog(@"[BLE-SENSORS] Remove all listeners");
    
    // Make sure to release the callback objects
    RELEASE_TO_NIL(statusCallback);
    RELEASE_TO_NIL(scanningCallback);
    RELEASE_TO_NIL(connectionCallback);
    RELEASE_TO_NIL(dataCallback);
}

/* Find the callback by name.
 */
-(KrollCallback *)findCallback:(NSString *)key {
    if ([key isEqualToString:STATUS_EVENT]) {
        return statusCallback;
        
    } else if ([key isEqualToString:SCANNING_EVENT]) {
        return scanningCallback;
        
    } else if ([key isEqualToString:CONNECTION_EVENT]) {
        return connectionCallback;
        
    } else if ([key isEqualToString:DATA_EVENT]) {
        return dataCallback;
    }
    
    return nil;
}

-(void)invokeCallback:(NSString*)callbackName withData:(NSMutableDictionary*)data {
    KrollCallback *callback = [self findCallback:callbackName];
    if (callback != nil) {
        TiThreadPerformOnMainThread(^{
            [callback call:[NSArray arrayWithObjects: data, nil] thisObject:self];
        }, YES);
    }
}

#else

-(void)invokeCallback:(NSString*)eventName withData:(NSDictionary*)eventData {
    if ([self _hasListeners:eventName]) {
        [self fireEvent:eventName withObject:[NSDictionary dictionaryWithObject:eventData forKey:@"data"]];
    }
}

#endif

-(NSString *)currentTimestamp {
    NSTimeInterval time = ([[NSDate date] timeIntervalSince1970]); // returned as a double
    long digits = (long)time; // this is the first 10 digits
    int decimalDigits = (int)(fmod(time, 1) * 1000); // this will get the 3 missing digits
    long timestamp = (digits * 1000) + decimalDigits;
    
    return [NSString stringWithFormat:@"%ld%d", digits, decimalDigits];
}

/* Scan for bluetooth ble devices, returning the current status
 */
-(void)startDiscovery:(id)no_args {
    NSLog(@"[BLE-SENSORS] Scan for sensor devices");
    
    NSMutableDictionary *scanData = [NSMutableDictionary dictionary];
    [scanData setObject:@"discovery-started" forKey:@"action"];
    [self invokeCallback:SCANNING_EVENT withData:scanData];
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    ComSwsSensorsBtBleModule *this = self;
    
    [centralManager startScanWithCallback:^(CBPeripheral *peripheral, NSDictionary *advertisementData, NSNumber *RSSI, NSString *inferredType) {
        if (([peripheral identifier] != nil) && (inferredType != nil)) {
            
            NSString *name = ([peripheral name] != nil) ? [peripheral name] : inferredType;
            
            NSMutableDictionary *scanData = [NSMutableDictionary dictionary];
            [scanData setObject:@"device-detected" forKey:@"action"];
            [scanData setObject:[this currentTimestamp] forKey:@"timestamp"];
            [scanData setObject:[[peripheral identifier] UUIDString] forKey:@"address"];
            [scanData setObject:RSSI forKey:@"rssi"];
            [scanData setObject:name forKey:@"name"];
            [scanData setObject:inferredType forKey:@"type"];
            
            // TODO ... The advertisement may contain ANY property of ANY type, for which it may not be possible to pass across the Kroll bridge.
            // This has already been shown to be a problem for CBUUID's, with the warning:
            // "Creating 'XXXX...-XXXXXXXXX' in a different context than the calling function." being raised by KrollObject.
            //            [scanData setObject:advertisementData forKey:@"advertisement"];
            
            [this invokeCallback: SCANNING_EVENT withData:scanData];
        }
    } aggressive:YES];
}

-(void)cancelDiscovery:(id)no_args {
    NSLog(@"[BLE-SENSORS] Stop discovery of sensor devices");
    
    NSMutableDictionary *scanData = [NSMutableDictionary dictionary];
    [scanData setObject:@"discovery-stopped" forKey:@"action"];
    [self invokeCallback: SCANNING_EVENT withData:scanData];
    
    [[DEACentralManager sharedService] stopScan];
}

-(id)isConnected:(id)uuid {
    ENSURE_SINGLE_ARG(uuid, NSString);
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    NSUUID * nsuuid = [[NSUUID UUID] initWithUUIDString:uuid];
    YMSCBPeripheral *peripheral = [centralManager findPeripheralByUUID: nsuuid];
    BOOL connected = [peripheral isConnected];
    
    NSLog(@"[BLE-SENSORS] Device connection: %@", uuid, connected);
    
    return NUMBOOL(connected);
}

-(void)connect:(NSArray *)args {
    NSString *uuid;
    ENSURE_ARG_AT_INDEX(uuid, args, 0, NSString);
    NSUUID * nsuuid = [[NSUUID UUID] initWithUUIDString:uuid];
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    
    if ([args count] > 1) {
        NSString *hint;
        ENSURE_ARG_AT_INDEX(hint, args, 1, NSString);
        
        [centralManager addPeripheralHint:uuid hint:hint];
        [centralManager retrievePeripheralsWithIdentifiers:[NSArray arrayWithObjects: nsuuid, nil]];
        
        NSLog(@"[BLE-SENSORS] Connecting to device: %@ as %@", uuid, hint);
    } else {
        [centralManager retrievePeripheralsWithIdentifiers:[NSArray arrayWithObjects: nsuuid, nil]];
        
        NSLog(@"[BLE-SENSORS] Connecting to device: %@", uuid);
    }
}

-(void)disconnect:(id)uuid {
    ENSURE_SINGLE_ARG(uuid, NSString);
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    NSUUID * nsuuid = [[NSUUID UUID] initWithUUIDString:uuid];
    [[centralManager findPeripheralByUUID: nsuuid] cancelConnection];
    
    NSLog(@"[BLE-SENSORS] Disconnected from device: %@", uuid);
}

-(void)update:(NSArray *)args {
    NSString *uuid;
    NSDictionary *values;
    
    ENSURE_ARG_AT_INDEX(uuid, args, 0, NSString);
    ENSURE_ARG_AT_INDEX(values, args, 1, NSDictionary);
    
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    NSUUID * nsuuid = [[NSUUID UUID] initWithUUIDString:uuid];
    YMSCBPeripheral *peripheral = [centralManager findPeripheralByUUID: nsuuid];
    
    if ([peripheral isConnected] == YES) {
        if ([values objectForKey:@"service"]) {
            // TODO ... generify.
            NSString *serviceName = [values objectForKey:@"service"];
            if ([serviceName isEqualToString:SPREADER_SERVICE]) {
                if ([peripheral isKindOfClass:[BLEMultispread class]]) {
                    BLESpreaderService *spreader = [((BLEMultispread *)peripheral) spreader];
                    [spreader updateCharacteristics:values];
                    
                    NSLog(@"[BLE-SENSORS] Update %@ with %@", uuid, values);
                } else {
                    NSLog(@"[BLE-SENSORS] Attempt to update %@ as a spreader service", uuid);
                }
                
            } else {
                NSLog(@"[BLE-SENSORS] Attempt to update %@ to unknown service %@", uuid, serviceName);
            }
        } else {
            NSLog(@"[BLE-SENSORS] Attempt to update %@, but no service is specified", uuid);
        }
        
    } else {
        NSLog(@"[BLE-SENSORS] Attempt to update %@ when not connected", uuid);
    }
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    
    if ([keyPath isEqualToString:@"isScanning"]) {
        DEACentralManager *centralManager = [DEACentralManager sharedService];
        if (centralManager.isScanning) {
            NSLog(@"[BLE-SENSORS] Scanning started for bluetooth devices");
        } else {
            NSLog(@"[BLE-SENSORS] Scanning stopped for bluetooth devices");
        }
        
    } else if ([keyPath isEqualToString:@"sensorValues"]) {
        if ([object isKindOfClass:[BLEBaseService class]]) {
            BLEBaseService *bs = (BLEBaseService *)object;
            YMSCBPeripheral *parent = [bs parent];
            
            NSMutableDictionary *data = [NSMutableDictionary dictionary];
            [data setObject: [self currentTimestamp] forKey:@"timestamp"];
            [data setObject: [parent name] forKey:@"name"];
            [data setObject: [[[parent cbPeripheral] identifier] UUIDString] forKey:@"address"];
            [data setObject: [bs name] forKey:@"service"];
            [data setObject: @"sensors" forKey:@"type"];
            [data setObject: change[@"new"] forKey:@"values"];
            [self invokeCallback:DATA_EVENT withData:data];
        }
        
    } else if ([keyPath isEqualToString:@"calibrationValues"]) {
        if ([object isKindOfClass:[BLEBaseService class]]) {
            BLEBaseService *bs = (BLEBaseService *)object;
            YMSCBPeripheral *parent = [bs parent];
            
            NSMutableDictionary *data = [NSMutableDictionary dictionary];
            [data setObject: [self currentTimestamp] forKey:@"timestamp"];
            [data setObject: [parent name] forKey:@"name"];
            [data setObject: [[[parent cbPeripheral] identifier] UUIDString] forKey:@"address"];
            [data setObject: [bs name] forKey:@"service"];
            [data setObject: @"calibrate" forKey:@"type"];
            [data setObject: change[@"new"] forKey:@"values"];
            [self invokeCallback:DATA_EVENT withData:data];
        }
    }
}

- (void)configureObserverForPeripheral:(CBPeripheral *)peripheral {
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    YMSCBPeripheral *yp = [centralManager findPeripheral:peripheral];
    for (CBService *service in peripheral.services) {
        YMSCBService *ys = [yp findService:service];
        if ([ys isKindOfClass:[BLEBaseService class]]) {
            BLEBaseService *bs = (BLEBaseService *)ys;
            [bs addObserver:self
                 forKeyPath:@"sensorValues"
                    options:NSKeyValueObservingOptionNew
                    context:NULL];
            [bs addObserver:self
                 forKeyPath:@"calibrationValues"
                    options:NSKeyValueObservingOptionNew
                    context:NULL];
        }
    }
}

-(void)deconfigureObserverForPeripheral:(CBPeripheral *)peripheral {
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    YMSCBPeripheral *yp = [centralManager findPeripheral:peripheral];
    for (CBService *service in peripheral.services) {
        YMSCBService *ys = [yp findService:service];
        if ([ys isKindOfClass:[BLEBaseService class]]) {
            BLEBaseService *bs = (BLEBaseService *)ys;
            [bs removeObserver:self forKeyPath:@"calibrationValues"];
            [bs removeObserver:self forKeyPath:@"sensorValues"];
            [bs disableNotifications];
        }
    }
}

-(void)centralManagerDidUpdateState:(CBCentralManager *)central {
    NSMutableDictionary *statusData = [NSMutableDictionary dictionary];
    
    [statusData setObject:[self currentTimestamp] forKey:@"timestamp"];
    
    if ([central state] == CBCentralManagerStatePoweredOff) {
        [statusData setObject:@"off" forKey:@"status"];
        [statusData setObject:@"Bluetooth is powered off" forKey:@"label"];
    }
    
    else if ([central state] == CBCentralManagerStatePoweredOn) {
        [statusData setObject:@"ready" forKey:@"status"];
        [statusData setObject:@"Bluetooth is powered on and ready" forKey:@"label"];
    }
    else if ([central state] == CBCentralManagerStateUnauthorized) {
        [statusData setObject:@"unauthorised" forKey:@"status"];
        [statusData setObject:@"Bluetooth is unauthorized" forKey:@"label"];
    }
    else if ([central state] == CBCentralManagerStateUnknown) {
        [statusData setObject:@"unknown" forKey:@"status"];
        [statusData setObject:@"Mysterious status" forKey:@"label"];
    }
    else if ([central state] == CBCentralManagerStateUnsupported) {
        [statusData setObject:@"unsupported" forKey:@"status"];
        [statusData setObject:@"Bluetooth in unsupported state" forKey:@"label"];
        
    } else {
        [statusData setObject:@"unknown" forKey:@"status"];
        [statusData setObject:@"Mysterious status" forKey:@"label"];
    }
    
    [self invokeCallback:STATUS_EVENT withData:statusData];
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    if (error) {
        return;
    }
    
    [self configureObserverForPeripheral:peripheral];
}

- (void)centralManager:(CBCentralManager *)central didRetrievePeripherals:(NSArray *)peripherals {
    DEACentralManager *centralManager = [DEACentralManager sharedService];
    
    unsigned long count = (unsigned long)[peripherals count];
    if (count > 0) {
        CBPeripheral *peripheral = [peripherals objectAtIndex:0];
        YMSCBPeripheral *yp = [centralManager findPeripheral:peripheral];
        
        if ([yp isKindOfClass:[YMSCBPeripheral class]]) {
            YMSCBPeripheral *ymsPeripheral = (YMSCBPeripheral *)yp;
            ymsPeripheral.delegate = self;
            ymsPeripheral.watchdogTimerInterval = 30.0;
            [ymsPeripheral connect];
            
        } else {
            //            NSLog(@"Unknown perhipheral");
        }
    }
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    if ([peripheral state] == CBPeripheralStateConnected) {
        requestedConnection = nil;

        NSMutableDictionary *data = [NSMutableDictionary dictionary];
        [data setObject:[self currentTimestamp] forKey:@"timestamp"];
        [data setObject:[[peripheral identifier] UUIDString] forKey:@"address"];
        [data setObject:@"connected" forKey:@"status"];
        [self invokeCallback:CONNECTION_EVENT withData:data];
    }
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    [self deconfigureObserverForPeripheral:peripheral];
    requestedConnection = nil;
    
    NSMutableDictionary *data = [NSMutableDictionary dictionary];
    [data setObject:[self currentTimestamp] forKey:@"timestamp"];
    [data setObject:[[peripheral identifier] UUIDString] forKey:@"address"];
    [data setObject:@"disconnected" forKey:@"status"];
    [self invokeCallback:CONNECTION_EVENT withData:data];
}

@end
