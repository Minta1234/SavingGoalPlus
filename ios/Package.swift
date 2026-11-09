// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SavingGoalPlus",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "SavingGoalPlusCore",
            targets: ["SavingGoalPlusCore"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "SavingGoalPlusCore",
            path: "SavingGoalPlus"
        ),
        .testTarget(
            name: "SavingGoalPlusTests",
            dependencies: ["SavingGoalPlusCore"],
            path: "SavingGoalPlusTests"
        )
    ]
)

