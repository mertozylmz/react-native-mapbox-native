require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-mapbox-native"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-mapbox-native
                   DESC
  s.homepage     = "https://github.com/mertozylmz/react-native-mapbox-native"
  s.license      = "MIT"
  # s.license    = { :type => "MIT", :file => "FILE_LICENSE" }
  s.authors      = { "Mert Ozyilmaz" => "mertozyilmazz@gmail.com" }
  s.platforms    = { :ios => "9.0", :tvos => "10.0" }
  s.source       = { :git => "https://github.com/mertozylmz/react-native-mapbox-native.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency 'Mapbox-iOS-SDK', '~> 5.4'
  s.dependency 'MapboxNavigation', '~> 0.38.0'
end

