import SwiftUI

public struct CircularGoalGaugeView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    public let progressFraction: Double // 0.0 ... 1.0
    public let savedAmount: Double
    public let targetAmount: Double
    public var size: CGFloat = 220

    public init(
        progressFraction: Double,
        savedAmount: Double,
        targetAmount: Double,
        size: CGFloat = 220
    ) {
        self.progressFraction = min(1.0, max(0.0, progressFraction))
        self.savedAmount = savedAmount
        self.targetAmount = targetAmount
        self.size = size
    }

    private var percentage: Int {
        Int((progressFraction * 100.0).rounded())
    }

    private var stageColor: Color {
        ColorTheme.stageColor(for: percentage)
    }

    // 260-degree arc: start at 140°, end at 400° (clockwise)
    private let startAngle = Angle(degrees: 140)
    private let sweepAngle: Double = 260.0

    public var body: some View {
        ZStack {
            // Track Arc
            ArcShape(startAngle: startAngle, sweepDegrees: sweepAngle)
                .stroke(
                    ColorTheme.trackBackground,
                    style: StrokeStyle(lineWidth: size * 0.08, lineCap: .round)
                )

            // Progress Arc
            ArcShape(startAngle: startAngle, sweepDegrees: sweepAngle * progressFraction)
                .stroke(
                    stageColor,
                    style: StrokeStyle(lineWidth: size * 0.08, lineCap: .round)
                )
                .animation(.spring(response: 0.8, dampingFraction: 0.7), value: progressFraction)

            // Inner Info
            VStack(spacing: 4) {
                Text("\(percentage)%")
                    .font(.system(size: size * 0.18, weight: .bold, design: .rounded))
                    .foregroundColor(stageColor)

                Text(SavingsCalculations.formatCurrency(savedAmount, currencySymbol: currencySymbol))
                    .font(.system(size: size * 0.11, weight: .semibold, design: .rounded))
                    .foregroundColor(.primary)

                Text("of \(SavingsCalculations.formatCurrency(targetAmount, currencySymbol: currencySymbol))")
                    .font(.system(size: size * 0.065, weight: .medium))
                    .foregroundColor(.secondary)
            }
        }
        .frame(width: size, height: size)
    }
}

private struct ArcShape: Shape {
    let startAngle: Angle
    let sweepDegrees: Double

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2.0 - 12.0
        let endAngle = Angle(degrees: startAngle.degrees + sweepDegrees)

        path.addArc(
            center: center,
            radius: radius,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: false
        )
        return path
    }
}

