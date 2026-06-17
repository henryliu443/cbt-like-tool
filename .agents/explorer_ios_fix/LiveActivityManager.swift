import Foundation
import ActivityKit
import shared

@available(iOS 16.1, *)
class LiveActivityManager {
    static let shared = LiveActivityManager()
    private var activity: Activity<ExerciseAttributes>?
    
    func start(exerciseName: String, state: ExerciseSessionState, totalCycles: Int) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        
        let attributes = ExerciseAttributes(exerciseName: exerciseName)
        let contentState = ExerciseAttributes.ContentState(
            phaseDisplayName: state.phase.displayName,
            remainingTime: Int(state.remainingTime),
            cycle: Int(state.cycle),
            totalCycles: totalCycles
        )
        
        do {
            activity = try Activity.request(attributes: attributes, contentState: contentState, pushType: nil)
        } catch {
            print("Failed to start Live Activity: \(error)")
        }
    }
    
    func update(state: ExerciseSessionState, totalCycles: Int) {
        guard let activity = activity else { return }
        let contentState = ExerciseAttributes.ContentState(
            phaseDisplayName: state.phase.displayName,
            remainingTime: Int(state.remainingTime),
            cycle: Int(state.cycle),
            totalCycles: totalCycles
        )
        let activityContent = ActivityContent(state: contentState, staleDate: nil)
        Task {
            await activity.update(activityContent)
        }
    }
    
    func end() {
        guard let activity = activity else { return }
        Task {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
        self.activity = nil
    }
}
