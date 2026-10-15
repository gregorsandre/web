import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { ChatProvider } from './chat/ChatProvider'
import { AppShell } from './layout/AppShell'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { ChatsPage } from './pages/ChatPages'
import { ConnectionsPage, RecommendationsPage } from './pages/ListPages'
import { ProfilePage } from './pages/ProfilePage'
import { NotFoundPage, UserPage } from './pages/UserPage'
import { ProfileStatusProvider } from './profile/ProfileStatusProvider'
import { RequireAuth, RequireGuest, RequireProfile } from './routes/guards'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<RequireGuest />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Route>

          <Route element={<RequireAuth />}>
            <Route element={<SignedIn />}>
              <Route element={<AppShell />}>
                <Route index element={<Navigate to="/recommendations" replace />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route element={<RequireProfile />}>
                  <Route path="/recommendations" element={<RecommendationsPage />} />
                  <Route path="/connections" element={<ConnectionsPage />} />
                  <Route path="/chats" element={<ChatsPage />} />
                  <Route path="/users/:id" element={<UserPage />} />
                </Route>
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

// Mounted only while logged in, so the profile check and the chat socket restart for each session
function SignedIn() {
  return (
    <ProfileStatusProvider>
      <ChatProvider>
        <Outlet />
      </ChatProvider>
    </ProfileStatusProvider>
  )
}

export default App
