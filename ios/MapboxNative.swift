//
//  MapboxNative.swift
//  react-native-mapbox-native
//
//  Created by Mert Ozyilmaz on 1.10.2019.
//

import Foundation
import Mapbox
import MapboxCoreNavigation
import MapboxDirections
import MapboxNavigationNative
import MapboxNavigation

class MyCustomPointAnnotation: MGLPointAnnotation {
    var isHotspot: Bool = false
}

class MyCustomPolygon: MGLPolygon {
    var isHotspot: Bool = false
}

@objc(MapboxNative)
class MapboxNative: RCTViewManager, MGLMapViewDelegate, NavigationViewControllerDelegate {
    let rootViewController:UIViewController? = UIApplication.shared.delegate?.window??.rootViewController!
    var customUserAnnotation = CustomUserLocationAnnotationView()
    var marker = MyCustomPointAnnotation()
    var coordinates = [NSObject]()
    var mapView = MGLMapView()
    
    // Render MapView
    override func view() -> UIView! {
        mapView = MGLMapView(frame: (self.rootViewController?.view.bounds)!)
        mapView.delegate = self
        // Map Settings
        mapView.userTrackingMode = .followWithHeading
        mapView.showsUserHeadingIndicator = true
        mapView.logoView.isHidden = true
        mapView.compassView.isHidden = true
        mapView.isPitchEnabled = false
        mapView.styleURL = MGLStyle.lightStyleURL
        
        return mapView
    }
    
    // Tap the user location annotation to toggle heading tracking mode.
    func mapView(_ mapView: MGLMapView, didDeselect annotation: MGLAnnotation) {
        if (annotation is MGLUserLocation) {
            if (mapView.userTrackingMode != .followWithHeading) {
                mapView.userTrackingMode = .followWithHeading
            } else {
                mapView.resetNorth()
            }
            mapView.deselectAnnotation(annotation, animated: false)
        } else if let pointAnnotation = annotation as? MyCustomPointAnnotation {
            if (pointAnnotation.isHotspot) {
                hotspotEvent(value: [pointAnnotation.coordinate.longitude, pointAnnotation.coordinate.latitude])
            }
        } else if let polygonAnnotation = annotation as? MyCustomPolygon {
            if (polygonAnnotation.isHotspot) {
                hotspotEvent(value: [polygonAnnotation.coordinate.longitude, polygonAnnotation.coordinate.latitude])
            }
        }
    }
    
    // Create Custom User Annotation
    func mapView(_ mapView: MGLMapView, viewFor annotation: MGLAnnotation) -> MGLAnnotationView? {
        if annotation is MGLUserLocation && mapView.userLocation != nil {
            return customUserAnnotation
        }
        return nil
    }
    
    // Adding Point Set Image
    func mapView(_ mapView: MGLMapView, imageFor annotation: MGLAnnotation) -> MGLAnnotationImage? {
        if let castAnnotation = annotation as? MyCustomPointAnnotation {
            if (castAnnotation.isHotspot) {
                var annotationImage = mapView.dequeueReusableAnnotationImage(withIdentifier: "hotspot")
                if annotationImage == nil {
                    let image = UIImage(named: "Hotspot")!
                    annotationImage = MGLAnnotationImage(image: image, reuseIdentifier: "hotspot")
                }
                return annotationImage
            }
        }
        
        var annotationImage = mapView.dequeueReusableAnnotationImage(withIdentifier: "pin")
        if annotationImage == nil {
            let image = UIImage(named: "Marker")!
            annotationImage = MGLAnnotationImage(image: image, reuseIdentifier: "pin")
        }
        return annotationImage
    }
    
    func mapView(_ mapView: MGLMapView, regionDidChangeAnimated animated: Bool) {
        if (mapView.userTrackingMode != .followWithHeading) {
            setEventValue(value: true)
        }
    }
    
    @objc func resetRegion() {
        DispatchQueue.main.async {
            self.mapView.userTrackingMode = .followWithHeading
        }
        setEventValue(value: false)
    }
    
    @objc func regionChangedEvent() {
        setEventValue(value: false)
    }
    
    // Add Point & Draw Route
    @objc func addPoint(_ coordinates: NSArray, setCamera camera: NSNumber, callback successCallback: @escaping RCTResponseSenderBlock) {
        let markerCoordinate = CLLocationCoordinate2D(
            latitude: CLLocationDegrees(truncating: coordinates[1] as! NSNumber),
            longitude: CLLocationDegrees(truncating: coordinates[0] as! NSNumber)
        )
        calculate(to: markerCoordinate, setCamera: camera, successCallback: successCallback)
        if camera.boolValue {
            marker.coordinate = markerCoordinate
            mapView.addAnnotation(marker)
        }
    }
    
    func calculate(to destinationCoor: CLLocationCoordinate2D, setCamera camera: NSNumber, successCallback: @escaping RCTResponseSenderBlock) {
        let origin = Waypoint(coordinate: mapView.userLocation!.coordinate, coordinateAccuracy: -1, name: "Start")
        let destination = Waypoint(coordinate: destinationCoor, coordinateAccuracy: -1, name: "Finish")
        let options = NavigationRouteOptions(waypoints: [origin, destination], profileIdentifier: MBDirectionsProfileIdentifier.automobile)
        
        Directions.shared.calculate(options) { (waypoints, routes, error) in
            guard let route = routes?.first else { return }
            
            self.drawRoute(route: route)
            
            var routeCoordinates = [NSArray]()
            for coord in route.coordinates! {
                routeCoordinates.append([coord.longitude, coord.latitude])
            }
            successCallback([routeCoordinates])
            
            if camera.boolValue {
                let coordinateBounds = MGLCoordinateBounds(sw: destinationCoor, ne: self.mapView.userLocation!.coordinate)
                let insets = UIEdgeInsets(top: 80, left: 50, bottom: 150, right: 50)
                let routeCam = self.mapView.cameraThatFitsCoordinateBounds(coordinateBounds, edgePadding: insets)
                self.mapView.setCamera(routeCam, animated: true)
                self.mapView.userTrackingMode = .followWithHeading
            }
        }
    }
    
    // Draw Route
    func drawRoute(route: Route) {
        mapView.setUserTrackingMode(.none, animated: true, completionHandler: nil)
        guard route.coordinateCount > 0 else { return }
        var routeCoordinates = route.coordinates!
        let polyline = MGLPolylineFeature(coordinates: &routeCoordinates, count: route.coordinateCount)
        
        if let source = mapView.style?.source(withIdentifier: "route-source") as? MGLShapeSource {
            source.shape = polyline
        } else {
            let source = MGLShapeSource(identifier: "route-source", features: [polyline])
            let lineStyle = MGLLineStyleLayer(identifier: "route-style", source: source)
            
            lineStyle.lineColor = NSExpression(forConstantValue: #colorLiteral(red: 0.3607843137, green: 0.3607843137, blue: 0.3607843137, alpha: 1))
            lineStyle.lineWidth = NSExpression(forConstantValue: 4)
            lineStyle.lineCap = NSExpression(forConstantValue: "round")
            lineStyle.lineJoin = NSExpression(forConstantValue: "round")
            
            mapView.style?.addSource(source)
            mapView.style?.addLayer(lineStyle)
        }
        mapView.userTrackingMode = .followWithHeading
    }
    
    // Add Polygon Center Point
    @objc func polygonCenterPoint(_ coordinates: NSArray) {
        let polygonMarker = MyCustomPointAnnotation()
        polygonMarker.isHotspot = true
        let markerCoordinate = CLLocationCoordinate2D(
            latitude: CLLocationDegrees(truncating: coordinates[1] as! NSNumber),
            longitude: CLLocationDegrees(truncating: coordinates[0] as! NSNumber)
        )
        polygonMarker.coordinate = markerCoordinate
        mapView.addAnnotation(polygonMarker)
    }

    // Draw Polygon
    @objc func drawPolygon(_ coordinateList: NSArray, isHotspot: NSNumber = 0) {
        var coords = [CLLocationCoordinate2D]()
        for i in 0...coordinateList.count - 1 {
            let coord: [NSObject] = coordinateList[i] as! [NSObject]
            if coord.count > 1 {
               coords.append(CLLocationCoordinate2D(
                    latitude: CLLocationDegrees(truncating: coord[1] as! NSNumber),
                    longitude: CLLocationDegrees(truncating: coord[0] as! NSNumber)
               ))
            }
        }
        let shape = MyCustomPolygon(coordinates: &coords, count: UInt(coords.count))
        shape.isHotspot = isHotspot.boolValue
        DispatchQueue.main.async {
            self.mapView.addAnnotation(shape)
        }
    }
    
    func mapView(_ mapView: MGLMapView, strokeColorForShapeAnnotation annotation: MGLShape) -> UIColor {
        return UIColor(red: 1/255, green: 122/255, blue: 255/255, alpha: 1)
    }
    
    func mapView(_ mapView: MGLMapView, fillColorForPolygonAnnotation annotation: MGLPolygon) -> UIColor {
        if let polygonAnnotation = annotation as? MyCustomPolygon {
            if (polygonAnnotation.isHotspot) {
                    return UIColor(red: 1/255, green: 122/255, blue: 255/255, alpha: 0.20)
            }
            return UIColor(red: 1/255, green: 122/255, blue: 255/255, alpha: 0.10)
        }
        return UIColor(red: 1/255, green: 122/255, blue: 255/255, alpha: 0.10)
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        self.rootViewController!.dismiss(animated: true, completion: nil)
        return false
    }
    
    // Clear
    @objc func clearMapItems() {
        DispatchQueue.main.async {
            self.mapView.removeAnnotation(self.marker)
        }
        if let source = mapView.style?.source(withIdentifier: "route-source") as? MGLShapeSource {
            mapView.style?.removeSource(source)
        }
        if let lineStyle = mapView.style?.layer(withIdentifier: "route-style") as? MGLLineStyleLayer {
            mapView.style?.removeLayer(lineStyle)
        }
    }
    
    // Turn By Turn - Navigation Coordinates Set
    @objc func setCoordinates(_ coordinateList: NSArray) {
        coordinates.removeAll()
        for i in 0...coordinateList.count - 1 {
            let coordinate: [NSObject] = coordinateList[i] as! [NSObject]
            if coordinate.count > 1 {
                coordinates.append(
                    Waypoint(coordinate: CLLocationCoordinate2D(
                        latitude: CLLocationDegrees(truncating: coordinate[1] as! NSNumber),
                        longitude: CLLocationDegrees(truncating: coordinate[0] as! NSNumber)
                    ))
                )
            }
        }
    }
    
    // Turn By Turn - Navigation Start
    @objc func startNavigation() {
        let options = NavigationRouteOptions(waypoints: coordinates as! [Waypoint], profileIdentifier: MBDirectionsProfileIdentifier.automobile)
        options.roadClassesToAvoid = .toll
        Directions.shared.calculate(options) { (waypoints, routes, error) in
            guard let route = routes?.first else { return }
            let viewController = NavigationViewController(for: route)
            
            viewController.showsEndOfRouteFeedback = false
            viewController.showsReportFeedback = false
            viewController.modalPresentationStyle = .fullScreen
            self.rootViewController!.present(viewController, animated: true, completion: nil)
        }
    }
    
    // Turn By Turn - Navigation Stop
    @objc func stopNavigation() {
        self.rootViewController!.dismiss(animated: true, completion: nil)
    }
    
    func setEventValue(value: Bool) {
        self.bridge.eventDispatcher()?.sendDeviceEvent(withName: "regionChanged", body: value)
    }
    
    func hotspotEvent(value: NSArray) {
        self.bridge.eventDispatcher()?.sendDeviceEvent(withName: "clickedHotspotZone", body: value)
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
