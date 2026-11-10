import SwiftUI

struct ServerConnectionView: View {
    let session: ServerSession
    @State private var serverURL: String
    @State private var email = ""
    @State private var password = ""

    init(session: ServerSession) {
        self.session = session
        _serverURL = State(initialValue: session.serverURL)
    }

    var body: some View {
        Form {
            Section(AppStrings.server) {
                TextField(AppStrings.serverURL, text: $serverURL)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()
                    .onChange(of: serverURL) { _, value in session.updateServerURL(value) }
                Button {
                    session.updateServerURL(serverURL)
                    serverURL = session.serverURL
                    Task { await session.validateServer() }
                } label: {
                    if session.isChecking { ProgressView() } else { Text(AppStrings.validateServer) }
                }
                .disabled(session.isChecking || serverURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                if let info = session.serverInfo {
                    LabeledContent(AppStrings.serverName, value: info.name)
                    LabeledContent(AppStrings.apiVersion, value: info.apiVersion)
                }
            }

            if session.requiresLogin {
                Section {
                    Text(AppStrings.loginAgain)
                        .foregroundStyle(.orange)
                }
            }

            if let info = session.serverInfo, !session.isLoggedIn {
                Section(AppStrings.login) {
                    TextField(AppStrings.email, text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                    SecureField(AppStrings.password, text: $password)
                    Button {
                        Task {
                            await session.login(email: email, password: password)
                            password = ""
                        }
                    } label: {
                        if session.isChecking { ProgressView() } else { Text(AppStrings.login) }
                    }
                    .disabled(session.isChecking || email.isEmpty || password.isEmpty || info.status != "ok")
                }
            }

            if session.isLoggedIn {
                Section(AppStrings.connected) {
                    LabeledContent(AppStrings.serverName, value: session.serverInfo?.name ?? "")
                    LabeledContent(AppStrings.account, value: session.accountEmail ?? "")
                    Button(AppStrings.logout, role: .destructive) { session.logout() }
                }
            }

            if let error = session.errorMessage {
                Section(AppStrings.error) {
                    Text(error).foregroundStyle(.red)
                }
            }
        }
        .navigationTitle(AppStrings.serverConnection)
        .task {
            session.refreshExpirationState()
            if session.serverInfo == nil { await session.validateServer() }
        }
    }
}
