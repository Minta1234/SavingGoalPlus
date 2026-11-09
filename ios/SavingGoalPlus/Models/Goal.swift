import Foundation

public struct Goal: Identifiable, Codable, Equatable, Hashable {
    public var id: Int64
    public var title: String
    public var targetAmount: Double
    public var savedAmount: Double
    public var targetDate: String // YYYY-MM-DD
    public var notes: String
    public var category: String
    public var photoPath: String?
    public var isArchived: Bool
    public var createdAt: Int64

    public init(
        id: Int64 = 0,
        title: String,
        targetAmount: Double,
        savedAmount: Double = 0.0,
        targetDate: String,
        notes: String = "",
        category: String = "General",
        photoPath: String? = nil,
        isArchived: Bool = false,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.title = title
        self.targetAmount = targetAmount
        self.savedAmount = savedAmount
        self.targetDate = targetDate
        self.notes = notes
        self.category = category
        self.photoPath = photoPath
        self.isArchived = isArchived
        self.createdAt = createdAt
    }

    public var remainingAmount: Double {
        max(0.0, targetAmount - savedAmount)
    }

    public var progressFraction: Double {
        guard targetAmount > 0 else { return 0.0 }
        return min(1.0, max(0.0, savedAmount / targetAmount))
    }

    public var progressPercentage: Int {
        Int((progressFraction * 100.0).rounded())
    }

    public var isCompleted: Bool {
        savedAmount >= targetAmount && targetAmount > 0
    }
}

