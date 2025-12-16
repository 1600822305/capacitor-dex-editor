// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorDexEditor",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorDexEditor",
            targets: ["DexEditorPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "DexEditorPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/DexEditorPluginPlugin"),
        .testTarget(
            name: "DexEditorPluginPluginTests",
            dependencies: ["DexEditorPluginPlugin"],
            path: "ios/Tests/DexEditorPluginPluginTests")
    ]
)