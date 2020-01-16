import { requireNativeComponent, NativeModules, Platform } from 'react-native';

const MapboxNative = requireNativeComponent('MapboxNative', null);

export const AddPoint = (coord, isCamera = true) => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.addPoint(coord, isCamera, () => {});
    }
}

export const SetCoordinates = (coords) => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.setCoordinates(coords);
    }
}

export const StartNavigation = () => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.startNavigation();
    }
}

export const StopNavigation = () => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.stopNavigation();
    }
}

export const ClearMap = () => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.clearMapItems();
    }
}

export const DrawPolygon = (coords, isHotspot = false) => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.drawPolygon(coords, isHotspot);
    }
}

export const PolygonCenterPoint = (coord) => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.polygonCenterPoint(coord);
    }
}

export const RegionChangedEvent = () => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.regionChangedEvent();
    }
}

export const ResetRegion = () => {
    if (Platform.OS == 'ios') {
        NativeModules.MapboxNative.resetRegion();
    }
}

export default MapboxNative;
