//
//  CloudCryptoComplication.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI
import WidgetKit

// MARK: - Complication Entry

struct ComplicationEntry: TimelineEntry {
    let date: Date
    let isRegistered: Bool
}

// MARK: - Complication Provider

struct CloudCryptoComplicationProvider: TimelineProvider {
    func placeholder(in context: Context) -> ComplicationEntry {
        ComplicationEntry(date: Date(), isRegistered: false)
    }

    func getSnapshot(in context: Context, completion: @escaping (ComplicationEntry) -> Void) {
        let entry = ComplicationEntry(
            date: Date(),
            isRegistered: UserDefaults.standard.bool(forKey: "isRegistered")
        )
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ComplicationEntry>) -> Void) {
        let isRegistered = UserDefaults.standard.bool(forKey: "isRegistered")

        let entry = ComplicationEntry(
            date: Date(),
            isRegistered: isRegistered
        )

        // Update timeline in 1 hour
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))

        completion(timeline)
    }
}

// MARK: - Complication View

struct CloudCryptoComplicationView: View {
    let entry: ComplicationEntry

    var body: some View {
        ZStack {
            AccessoryWidgetBackground()

            VStack(spacing: 2) {
                Image(systemName: entry.isRegistered ? "checkmark.shield.fill" : "shield")
                    .font(.system(size: 16))
                    .foregroundColor(entry.isRegistered ? .green : .gray)

                Text(entry.isRegistered ? "REG" : "---")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(entry.isRegistered ? .green : .gray)
            }
        }
    }
}

// MARK: - Widget Configuration

@main
struct CloudCryptoComplication: Widget {
    let kind: String = "CloudCryptoComplication"

    var body: some WidgetConfiguration {
        StaticConfiguration(
            kind: kind,
            provider: CloudCryptoComplicationProvider()
        ) { entry in
            CloudCryptoComplicationView(entry: entry)
        }
        .configurationDisplayName("Cloud Crypto")
        .description("Shows device registration status")
        .supportedFamilies([
            .accessoryCircular,
            .accessoryRectangular,
            .accessoryInline
        ])
    }
}

// MARK: - Preview

#Preview("Circular - Registered", as: .accessoryCircular) {
    CloudCryptoComplication()
} timeline: {
    ComplicationEntry(date: Date(), isRegistered: true)
    ComplicationEntry(date: Date(), isRegistered: false)
}
