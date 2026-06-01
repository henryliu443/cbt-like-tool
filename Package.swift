// swift-tools-version: 6.1
// This is a Skip (https://skip.dev) package for CBTReframe.
import PackageDescription

let package = Package(
    name: "cbt-like-tool-2",
    defaultLocalization: "en",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "CBTReframe", type: .dynamic, targets: ["CBTReframe"]),
    ],
    dependencies: [
        .package(url: "https://source.skip.tools/skip.git", from: "1.9.2"),
        .package(url: "https://source.skip.tools/skip-ui.git", from: "1.0.0")
    ],
    targets: [
        .target(name: "CBTReframe", dependencies: [
            .product(name: "SkipUI", package: "skip-ui")
        ], resources: [.process("Resources")], swiftSettings: [.swiftLanguageMode(.v5)], plugins: [.plugin(name: "skipstone", package: "skip")]),
        .testTarget(name: "CBTReframeTests", dependencies: [
            "CBTReframe",
            .product(name: "SkipTest", package: "skip")
        ], resources: [.process("Resources")], swiftSettings: [.swiftLanguageMode(.v5)], plugins: [.plugin(name: "skipstone", package: "skip")]),
    ]
)
