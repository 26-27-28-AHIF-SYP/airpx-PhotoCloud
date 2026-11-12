// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "airpix",
    platforms: [
        .iOS("26.0"),
        .macOS(.v14),
    ],
    products: [
        // An xtool project should contain exactly one library product,
        // representing the main app.
        .library(
            name: "airpix",
            targets: ["airpix"]
        ),
    ],
    dependencies: [
        .package(path: "../PhotoCloudSync"),
    ],
    targets: [
        .target(
            name: "airpix",
            dependencies: [.product(name: "PhotoCloudSync", package: "PhotoCloudSync")],
            resources: [
                .process("Resources"),
            ]
        ),
    ]
)
