import Foundation
import SwiftUI
import Combine

public final class SavingsStorageService: ObservableObject {
    public static let shared = SavingsStorageService()

    @Published public private(set) var goals: [Goal] = []
    @Published public private(set) var payments: [Payment] = []

    private let fileManager = FileManager.default
    private let dbFileName = "savinggoal_plus_db.json"

    private var dbFileURL: URL {
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documents.appendingPathComponent(dbFileName)
    }

    public init() {
        loadData()
        if !fileManager.fileExists(atPath: dbFileURL.path) {
            seedSampleGoals()
        }
    }

    // MARK: - Persistence
    public func loadData() {
        guard fileManager.fileExists(atPath: dbFileURL.path) else { return }
        do {
            let data = try Data(contentsOf: dbFileURL)
            let payload = try JSONDecoder().decode(BackupPayload.self, from: data)
            self.goals = payload.goals
            self.payments = payload.payments
        } catch {
            print("Failed to load local database: \(error.localizedDescription)")
        }
    }

    public func saveData() {
        do {
            let payload = BackupPayload(goals: goals, payments: payments)
            let encoder = JSONEncoder()
            encoder.outputFormatting = .prettyPrinted
            let data = try encoder.encode(payload)
            try data.write(to: dbFileURL, options: .atomic)
        } catch {
            print("Failed to save local database: \(error.localizedDescription)")
        }
    }

    // MARK: - Goal CRUD
    public func addGoal(
        title: String,
        targetAmount: Double,
        targetDate: String,
        notes: String = "",
        category: String = "General",
        photoPath: String? = nil
    ) {
        let newId = (goals.map(\.id).max() ?? 0) + 1
        let goal = Goal(
            id: newId,
            title: title,
            targetAmount: targetAmount,
            savedAmount: 0.0,
            targetDate: targetDate,
            notes: notes,
            category: category,
            photoPath: photoPath,
            isArchived: false
        )
        goals.insert(goal, at: 0)
        saveData()
    }

    public func updateGoal(_ goal: Goal) {
        if let index = goals.firstIndex(where: { $0.id == goal.id }) {
            goals[index] = goal
            saveData()
        }
    }

    public func deleteGoal(id: Int64) {
        goals.removeAll { $0.id == id }
        payments.removeAll { $0.goalId == id }
        saveData()
    }

    public func toggleArchive(id: Int64) {
        if let index = goals.firstIndex(where: { $0.id == id }) {
            goals[index].isArchived.toggle()
            saveData()
        }
    }

    // MARK: - Payment CRUD
    public func addPayment(goalId: Int64, amount: Double, notes: String, date: String = Payment.todayDateString()) -> Bool {
        guard let goalIndex = goals.firstIndex(where: { $0.id == goalId }) else { return false }
        let newPaymentId = (payments.map(\.id).max() ?? 0) + 1
        let payment = Payment(
            id: newPaymentId,
            goalId: goalId,
            amount: amount,
            date: date,
            notes: notes
        )
        payments.insert(payment, at: 0)

        // Recalculate goal savedAmount
        let currentSaved = goals[goalIndex].savedAmount
        let newSaved = max(0.0, currentSaved + amount)
        let wasNotComplete = currentSaved < goals[goalIndex].targetAmount
        goals[goalIndex].savedAmount = newSaved
        if amount > 0 {
            SavingsStreakManager.shared.recordDeposit()
        }
        saveData()

        let isNowComplete = newSaved >= goals[goalIndex].targetAmount
        return wasNotComplete && isNowComplete
    }

    public func deletePayment(id: Int64) {
        guard let payment = payments.first(where: { $0.id == id }) else { return }
        let goalId = payment.goalId
        payments.removeAll { $0.id == id }

        if let goalIndex = goals.firstIndex(where: { $0.id == goalId }) {
            let remainingPayments = payments.filter { $0.goalId == goalId }
            let totalSaved = remainingPayments.reduce(0.0) { $0 + $1.amount }
            goals[goalIndex].savedAmount = max(0.0, totalSaved)
        }
        saveData()
    }

    public func getPayments(forGoalId goalId: Int64) -> [Payment] {
        payments.filter { $0.goalId == goalId }.sorted { $0.createdAt > $1.createdAt }
    }

    // MARK: - Summary
    public func computeTotalSummary() -> TotalSummary {
        let activeGoals = goals.filter { !$0.isArchived }
        let totalTarget = activeGoals.reduce(0.0) { $0 + $1.targetAmount }
        let totalSaved = activeGoals.reduce(0.0) { $0 + $1.savedAmount }
        let totalRemaining = max(0.0, totalTarget - totalSaved)
        let overallPercentage = totalTarget > 0 ? Int(((totalSaved / totalTarget) * 100).rounded()) : 0
        let completed = activeGoals.filter { $0.isCompleted }.count

        return TotalSummary(
            totalTargetAmount: totalTarget,
            totalSavedAmount: totalSaved,
            totalRemainingAmount: totalRemaining,
            overallPercentage: overallPercentage,
            activeGoalCount: activeGoals.count,
            completedGoalCount: completed
        )
    }

    // MARK: - Backup & Restore
    public func exportBackupJson() -> String {
        let payload = BackupPayload(goals: goals, payments: payments)
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        if let data = try? encoder.encode(payload), let string = String(data: data, encoding: .utf8) {
            return string
        }
        return "{}"
    }

    static func readBackup(_ url: URL) throws -> BackupPayload {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        let file = try FileHandle(forReadingFrom: url)
        defer { try? file.close() }
        let limit = 16 * 1024 * 1024
        let data = try file.read(upToCount: limit + 1) ?? Data()
        guard data.count <= limit else { throw BackupImportError.tooLarge }

        let payload: BackupPayload
        do {
            payload = try JSONDecoder().decode(BackupPayload.self, from: data)
        } catch {
            throw BackupImportError.invalidFile
        }
        let goalIDs = Set(payload.goals.map(\.id))
        guard payload.version == 1,
              goalIDs.count == payload.goals.count,
              Set(payload.payments.map(\.id)).count == payload.payments.count,
              payload.goals.allSatisfy({ $0.id > 0 && $0.id < Int64.max && $0.targetAmount.isFinite && $0.savedAmount.isFinite }),
              payload.payments.allSatisfy({ $0.id > 0 && $0.id < Int64.max && goalIDs.contains($0.goalId) && $0.amount.isFinite })
        else { throw BackupImportError.invalidFile }
        return payload
    }

    func restoreBackup(_ payload: BackupPayload) throws {
        let data = try JSONEncoder().encode(payload)
        try data.write(to: dbFileURL, options: .atomic)
        goals = payload.goals
        payments = payload.payments
    }

    public func exportCsv() -> String {
        var csv = "Type,ID,GoalID,Title,TargetAmount,SavedAmount,Amount,Date,Notes,Category,IsArchived\n"
        for g in goals {
            csv += "GOAL,\(g.id),,\"\(g.title)\",\(g.targetAmount),\(g.savedAmount),,\(g.targetDate),\"\(g.notes)\",\"\(g.category)\",\(g.isArchived)\n"
        }
        for p in payments {
            csv += "PAYMENT,\(p.id),\(p.goalId),,,,,\(p.amount),\(p.date),\"\(p.notes)\",,\n"
        }
        return csv
    }

    private func seedSampleGoals() {
        let initialGoals = [
            Goal(id: 1, title: "Emergency Fund", targetAmount: 5000.0, savedAmount: 2500.0, targetDate: "2026-12-31", notes: "3 months expenses reserve", category: "Emergency"),
            Goal(id: 2, title: "Dream Vacation", targetAmount: 3000.0, savedAmount: 900.0, targetDate: "2026-08-15", notes: "Tokyo trip flights and hotels", category: "Travel"),
            Goal(id: 3, title: "New Laptop", targetAmount: 2200.0, savedAmount: 1800.0, targetDate: "2026-06-01", notes: "MacBook Pro for development", category: "Electronics")
        ]
        self.goals = initialGoals
        self.payments = [
            Payment(id: 1, goalId: 1, amount: 2500.0, notes: "Initial emergency deposit"),
            Payment(id: 2, goalId: 2, amount: 900.0, notes: "Bonus savings"),
            Payment(id: 3, goalId: 3, amount: 1800.0, notes: "Monthly tech budget")
        ]
        saveData()
    }
}



private enum BackupImportError: LocalizedError {
    case tooLarge, invalidFile

    var errorDescription: String? {
        switch self {
        case .tooLarge: return "Choose a backup file under 16 MB."
        case .invalidFile: return "This file is not a valid SavingGoalPlus iOS backup."
        }
    }
}
