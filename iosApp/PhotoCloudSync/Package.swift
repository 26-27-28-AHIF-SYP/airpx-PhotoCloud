// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "PhotoCloudSync",
    products: [.library(name: "PhotoCloudSync", targets: ["PhotoCloudSync"])],
    targets: [
        .target(name: "PhotoCloudSync"),
        .testTarget(name: "PhotoCloudSyncTests", dependencies: ["PhotoCloudSync"]),
    ]
)
