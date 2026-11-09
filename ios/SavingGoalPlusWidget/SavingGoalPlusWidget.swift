import WidgetKit
import SwiftUI

public struct SavingsWidgetEntry: TimelineEntry {
    public let date: Date
    public let totalSaved: Double
    public let totalRemaining: Double
    public let overallPercentage: Int

    public init(
        date: Date = Date(),
        totalSaved: Double = 5200.0,
        totalRemaining: Double = 4800.0,
        overallPercentage: Int = 52
    ) {
        self.date = date
        self.totalSaved = totalSaved
        self.totalRemaining = totalRemaining
        self.overallPercentage = overallPercentage
    }
}

public struct SavingsWidgetProvider: TimelineProvider {
    public func placeholder(in context: Context) -> SavingsWidgetEntry {
        SavingsWidgetEntry()
    }

    public func getSnapshot(in context: Context, completion: @escaping (SavingsWidgetEntry) -> Void) {
        completion(SavingsWidgetEntry())
    }

    public func getTimeline(in context: Context, completion: @escaping (Timeline<SavingsWidgetEntry>) -> Void) {
        let entry = SavingsWidgetEntry()
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date()
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        completion(timeline)
    }
}

public struct SavingsWidgetEntryView: View {
    public var entry: SavingsWidgetEntry

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("SAVING GOAL")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.secondary)
                    .tracking(1.0)
                Spacer()
                Text("\(entry.overallPercentage)%")
                    .font(.system(size: 11, weight: .bold, design: .rounded))
                    .foregroundColor(Color(red: 16/255, green: 185/255, blue: 129/255))
            }

            Text("$\(String(format: "%.2f", entry.totalSaved))")
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Spacer()

            HStack {
                Text("Left:")
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                Text("$\(String(format: "%.2f", entry.totalRemaining))")
                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                    .foregroundColor(.secondary)
            }
        }
        .padding(14)
    }
}

@main
public struct SavingGoalPlusWidget: Widget {
    public let kind: String = "SavingGoalPlusWidget"

    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SavingsWidgetProvider()) { entry in
            SavingsWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Savings Summary")
        .description("Track your total savings and remaining goals at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

