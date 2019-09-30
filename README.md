# react-native-mapbox-native

## Getting started

`$ npm install react-native-mapbox-native --save`

### Mostly automatic installation

`$ react-native link react-native-mapbox-native`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-mapbox-native` and add `MapboxNative.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libMapboxNative.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.MapboxNativePackage;` to the imports at the top of the file
  - Add `new MapboxNativePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-mapbox-native'
  	project(':react-native-mapbox-native').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-mapbox-native/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-mapbox-native')
  	```


## Usage
```javascript
import MapboxNative from 'react-native-mapbox-native';

// TODO: What to do with the module?
MapboxNative;
```
