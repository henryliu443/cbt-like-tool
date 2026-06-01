import SwiftUI

struct DisclaimerDetailView: View {
    @Environment(\.dismiss) private var dismiss
    var isSheet = true

    var body: some View {
        if isSheet {
            NavigationStack {
                content
            }
        } else {
            content
        }
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("CBT 思维重构 免责声明与使用条款")
                    .font(.title2.bold())
                    .padding(.bottom, 8)
                    .foregroundStyle(Color("TextPrimary"))

                disclaimerTextSection(
                    title: "一、非医疗服务声明",
                    content: "1. 本软件是一款基于认知行为治疗（CBT）理论的心理自助练习工具，仅作为个人进行日常情绪管理、心理调试和自我思维重构的辅助工具。\n2. 本软件所提供的所有功能、AI 分析回应、练习建议等，均不构成且不可替代专业的心理咨询、精神医学诊断、临床治疗或任何其他医疗建议。\n3. 开发者不具备提供临床医疗或心理治疗服务的资质，本软件亦不能替代医生或心理咨询师等专业人士的线下诊疗。"
                )

                disclaimerTextSection(
                    title: "二、心理危机与紧急情况",
                    content: "1. 本软件不具备实时心理危机干预、自杀预防或紧急求助监测功能。\n2. 如果您当前正处于严重的心理危机中，或有自残、自杀、伤害他人等极端想法或倾向，请立即停止使用本软件，并前往医院就诊或拨打专业心理援助热线：\n   • 全国心理援助热线：400-161-9995\n   • 紧急求助电话：110（报警）、120（急救）"
                )

                disclaimerTextSection(
                    title: "三、免责与责任限制",
                    content: "1. 本软件以“原样（AS IS）”提供，开发者在法律允许的最大范围内，不对本软件的功能完整性、AI 分析的绝对准确性、科学性、以及对特定个人的心理改善效果做出任何明示或暗示的保证。\n2. 人身安全与极端事件免责：用户使用本软件过程中的所有行为决定及其引发的后果均由用户自行承担。开发者对用户因使用或无法使用本软件而导致的任何形式的财产损失、身体健康损害、人身意外、自残、自杀或任何其他第三方起诉及法律责任，均不承担任何直接、间接、附带或特殊的赔偿或法律责任。\n3. 如果您对本软件的分析或功能有任何疑虑或产生不适感，应立即停止使用。"
                )

                disclaimerTextSection(
                    title: "四、隐私与数据安全",
                    content: "1. 本软件的所有数据（包括您的思维记录、情绪数据、API Key 等）均仅保存在您设备本地的 Keychain 及 SwiftData 数据库中，不上传至任何第三方开发者服务器。\n2. 您需要妥善保管您的设备，以防数据泄露。因设备丢失或被他人获取导致的数据泄露风险由您自行承担。"
                )
            }
            .padding()
        }
        .navigationTitle("免责声明与服务协议")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isSheet {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func disclaimerTextSection(title: String, content: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .foregroundStyle(Color("AccentColor"))
            Text(content)
                .font(.subheadline)
                .foregroundStyle(Color("TextSecondary"))
                .lineSpacing(6)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(.separator).opacity(0.1), lineWidth: 1)
        )
    }
}
