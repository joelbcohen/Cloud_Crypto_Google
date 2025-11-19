// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "CloudCrypto",
    platforms: [
        .watchOS(.v10)
    ],
    products: [
        .library(
            name: "CloudCrypto",
            targets: ["CloudCrypto"]
        )
    ],
    dependencies: [
        // No external dependencies - using native frameworks only
    ],
    targets: [
        .target(
            name: "CloudCrypto",
            dependencies: []
        )
    ]
)
