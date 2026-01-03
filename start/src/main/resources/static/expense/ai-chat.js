const API_BASE = '/expense/api/ai';
const SESSIONS_API = '/expense/api/ai/sessions';
let currentConversationId = null;
let sessions = [];
let currentUsername = null;

// ===== Authentication Functions =====

// Get JWT token from cookie
function getJwtToken() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'jwtToken') {
            return value;
        }
    }
    return null;
}

// Parse JWT token to get username (simple base64 decode)
function getUsernameFromToken(token) {
    try {
        const parts = token.split('.');
        if (parts.length === 3) {
            const payload = JSON.parse(atob(parts[1]));
            return payload.sub || payload.username;
        }
    } catch (e) {
        console.error('Failed to parse token:', e);
    }
    return null;
}

// Check login status on page load
function checkLoginStatus() {
    const token = getJwtToken();
    if (token) {
        const username = getUsernameFromToken(token);
        if (username) {
            showChatView(username);
            return;
        }
    }
    showAuthView();
}

// Show authentication view (login/register)
function showAuthView() {
    document.getElementById('authView').classList.remove('hidden');
    document.getElementById('chatContainer').style.display = 'none';
}

// Show chat view
function showChatView(username) {
    currentUsername = username;
    document.getElementById('authView').classList.add('hidden');
    document.getElementById('chatContainer').style.display = 'flex';
    // 不显示用户信息和登出按钮
    // document.getElementById('userInfo').style.display = 'flex';
    // document.getElementById('userName').textContent = username;

    // Initialize chat features
    loadSessions();
    restoreCurrentSession();
    loadSuggestions();
    setupInputHandlers();
}

// Switch to login form
function showLoginForm() {
    document.getElementById('loginForm').style.display = 'flex';
    document.getElementById('registerForm').style.display = 'none';
    document.getElementById('authTitle').textContent = '登录';
    document.getElementById('authSubtitle').textContent = '登录以使用 AI 记账助手';
    hideAuthError();
}

// Switch to register form
function showRegisterForm() {
    document.getElementById('loginForm').style.display = 'none';
    document.getElementById('registerForm').style.display = 'flex';
    document.getElementById('authTitle').textContent = '注册';
    document.getElementById('authSubtitle').textContent = '创建新账号以使用 AI 记账助手';
    hideAuthError();
}

// Show auth error message
function showAuthError(message) {
    const errorEl = document.getElementById('authError');
    errorEl.textContent = message;
    errorEl.classList.add('show');
}

// Hide auth error message
function hideAuthError() {
    const errorEl = document.getElementById('authError');
    errorEl.classList.remove('show');
}

// Handle login form submission
async function handleLogin(event) {
    event.preventDefault();
    hideAuthError();

    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const btn = document.getElementById('loginBtn');

    if (!username || !password) {
        showAuthError('请输入用户名和密码');
        return;
    }

    btn.disabled = true;
    btn.textContent = '登录中...';

    try {
        const response = await fetch('/expense/user/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ username, password })
        });

        const result = await response.json();

        if (result.status !== -1 && response.ok) {
            // Login successful
            showToast('登录成功', 'success');
            showChatView(username);
            // Initialize chat features
            loadSessions();
            restoreCurrentSession();
            loadSuggestions();
            setupInputHandlers();
        } else {
            showAuthError(result.msg || result.message || '登录失败，请检查用户名和密码');
        }
    } catch (error) {
        console.error('Login error:', error);
        showAuthError('网络错误，请稍后重试');
    } finally {
        btn.disabled = false;
        btn.textContent = '登录';
    }
}

// Handle register form submission
async function handleRegister(event) {
    event.preventDefault();
    hideAuthError();

    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;
    const btn = document.getElementById('registerBtn');

    if (!username || !email || !password) {
        showAuthError('请填写所有必填项');
        return;
    }

    if (password !== confirmPassword) {
        showAuthError('两次输入的密码不一致');
        return;
    }

    if (password.length < 6) {
        showAuthError('密码长度至少为6位');
        return;
    }

    btn.disabled = true;
    btn.textContent = '注册中...';

    try {
        const response = await fetch('/expense/user/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                username: username,
                email: email,
                password: password
            })
        });

        const result = await response.json();

        if (result.status !== -1 && response.ok) {
            // Registration successful, auto-login
            showToast('注册成功', 'success');
            showChatView(username);
            // Initialize chat features
            loadSessions();
            restoreCurrentSession();
            loadSuggestions();
            setupInputHandlers();
        } else {
            showAuthError(result.msg || result.message || '注册失败，请稍后重试');
        }
    } catch (error) {
        console.error('Register error:', error);
        showAuthError('网络错误，请稍后重试');
    } finally {
        btn.disabled = false;
        btn.textContent = '注册';
    }
}

// Handle logout
async function logout() {
    if (!confirm('确定要退出登录吗？')) {
        return;
    }

    try {
        // Clear the JWT cookie by setting it to expire
        document.cookie = 'jwtToken=; path=/; max-age=0';

        // Redirect to auth view
        currentUsername = null;
        showAuthView();
        showLoginForm();

        showToast('已退出登录', 'success');
    } catch (error) {
        console.error('Logout error:', error);
        showToast('退出登录失败', 'error');
    }
}

// ===== End Authentication Functions =====

// Configure marked
if (typeof marked !== 'undefined') {
    marked.setOptions({
        breaks: true,
        gfm: true,
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Check login status first
    checkLoginStatus();
});

function setupInputHandlers() {
    const inputField = document.getElementById('messageInput');

    // Auto-resize textarea
    inputField.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 150) + 'px';
    });

    // Enter to send, Shift+Enter for new line
    inputField.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Touch handling for better mobile experience
    if ('ontouchstart' in window) {
        document.body.style.touchAction = 'manipulation';
    }
}

// ===== Session Management =====

async function loadSessions() {
    try {
        const response = await fetch(SESSIONS_API, {
            credentials: 'include'
        });
        const result = await response.json();

        if (result.success && result.data) {
            sessions = result.data.sessions || [];
            renderSessionList();
        }
    } catch (error) {
        console.error('Failed to load sessions:', error);
    }
}

function renderSessionList() {
    const sessionList = document.getElementById('sessionList');

    if (sessions.length === 0) {
        sessionList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">💬</div>
                <div class="empty-state-text">暂无对话记录</div>
                <div class="empty-state-sub">点击上方按钮开始新对话</div>
            </div>
        `;
        return;
    }

    sessionList.innerHTML = sessions.map(session => `
        <div class="session-item ${session.conversationId === currentConversationId ? 'active' : ''}"
             onclick="switchSession('${session.conversationId}')"
             data-conversation-id="${session.conversationId}">
            <span class="session-icon">💬</span>
            <div class="session-info">
                <div class="session-title" id="title-${session.conversationId}">${escapeHtml(session.title)}</div>
                <div class="session-time">${formatTime(session.updatedAt || session.createdAt)}</div>
            </div>
            <div class="session-actions">
                <button class="session-action-btn" onclick="event.stopPropagation(); editSessionTitle('${session.conversationId}')" title="重命名">✏️</button>
                <button class="session-action-btn delete" onclick="event.stopPropagation(); confirmDeleteSession('${session.conversationId}')" title="删除">🗑️</button>
            </div>
        </div>
    `).join('');
}

function createNewSession() {
    currentConversationId = null;
    saveCurrentSession();
    clearMessages();
    showWelcomeScreen();
    closeSidebar();
    document.getElementById('messageInput').focus();
}

async function switchSession(conversationId) {
    if (conversationId === currentConversationId) {
        closeSidebar();
        return;
    }

    currentConversationId = conversationId;
    saveCurrentSession();

    // Update active state
    document.querySelectorAll('.session-item').forEach(item => {
        item.classList.toggle('active', item.dataset.conversationId === conversationId);
    });

    // Load messages
    await loadSessionMessages(conversationId);

    // Load suggestions for the switched session
    loadSuggestions();

    closeSidebar();
}

async function loadSessionMessages(conversationId) {
    try {
        const response = await fetch(`${SESSIONS_API}/${conversationId}/messages`, {
            credentials: 'include'
        });
        const result = await response.json();

        if (result.success && result.data) {
            clearMessages();
            const messages = result.data.messages || [];

            if (messages.length === 0) {
                showWelcomeScreen();
                return;
            }

            hideWelcomeScreen();
            messages.forEach(msg => {
                addMessageToUI(msg.content, msg.role === 'user', msg.createdAt);
            });
            scrollToBottom();
        }
    } catch (error) {
        console.error('Failed to load messages:', error);
        showToast('加载消息失败', 'error');
    }
}

async function confirmDeleteSession(conversationId) {
    if (!confirm('确定要删除这个对话吗？此操作不可恢复。')) {
        return;
    }

    try {
        const response = await fetch(`${SESSIONS_API}/${conversationId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.ok) {
            sessions = sessions.filter(s => s.conversationId !== conversationId);
            renderSessionList();

            // If deleted current session, create new one
            if (conversationId === currentConversationId) {
                createNewSession();
            }

            showToast('对话已删除', 'success');
        } else {
            showToast('删除失败', 'error');
        }
    } catch (error) {
        console.error('Failed to delete session:', error);
        showToast('删除失败', 'error');
    }
}

function editSessionTitle(conversationId) {
    const titleElement = document.getElementById(`title-${conversationId}`);
    const currentTitle = titleElement.textContent;

    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentTitle;
    input.className = 'session-title-input';
    input.maxLength = 200;

    titleElement.innerHTML = '';
    titleElement.appendChild(input);
    input.focus();
    input.select();

    const saveTitle = async () => {
        const newTitle = input.value.trim() || currentTitle;

        try {
            const response = await fetch(`${SESSIONS_API}/${conversationId}/rename`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ title: newTitle })
            });

            if (response.ok) {
                // Update local sessions
                const session = sessions.find(s => s.conversationId === conversationId);
                if (session) {
                    session.title = newTitle;
                }
                renderSessionList();
                showToast('标题已更新', 'success');
            } else {
                renderSessionList();
                showToast('更新失败', 'error');
            }
        } catch (error) {
            console.error('Failed to rename session:', error);
            renderSessionList();
            showToast('更新失败', 'error');
        }
    };

    input.addEventListener('blur', saveTitle);
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            input.blur();
        } else if (e.key === 'Escape') {
            renderSessionList();
        }
    });
}

function saveCurrentSession() {
    localStorage.setItem('ai_current_conversation', currentConversationId || '');
}

function restoreCurrentSession() {
    const saved = localStorage.getItem('ai_current_conversation');
    if (saved && saved !== '') {
        // Find session in loaded sessions
        const session = sessions.find(s => s.conversationId === saved);
        if (session) {
            currentConversationId = saved;
            loadSessionMessages(saved);
        }
    }
}

// ===== Sidebar Toggle =====

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    sidebar.classList.toggle('open');
    overlay.classList.toggle('show');
}

function closeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    sidebar.classList.remove('open');
    overlay.classList.remove('show');
}

// ===== Message Handling =====

function hideWelcomeScreen() {
    const welcomeScreen = document.getElementById('welcomeScreen');
    if (welcomeScreen) {
        welcomeScreen.style.display = 'none';
    }
}

function showWelcomeScreen() {
    const welcomeScreen = document.getElementById('welcomeScreen');
    if (welcomeScreen) {
        welcomeScreen.style.display = 'flex';
    }
}

function clearMessages() {
    const messagesArea = document.getElementById('messagesArea');
    // Keep welcome screen, remove messages
    const messages = messagesArea.querySelectorAll('.message');
    messages.forEach(msg => msg.remove());
    showWelcomeScreen();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function renderMarkdown(text) {
    if (typeof marked !== 'undefined') {
        try {
            return marked.parse(text);
        } catch (e) {
            console.error('Markdown parsing error:', e);
            return escapeHtml(text);
        }
    }
    // Fallback: simple text formatting
    return escapeHtml(text)
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>');
}

function addMessageToUI(content, isUser, createdAt) {
    hideWelcomeScreen();
    const messagesArea = document.getElementById('messagesArea');

    const time = createdAt ? new Date(createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
                                   : new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'ai'}`;

    const renderedContent = isUser ? escapeHtml(content) : renderMarkdown(content);

    messageDiv.innerHTML = `
        <div class="message-avatar">${isUser ? '👤' : '🤖'}</div>
        <div class="message-content">
            <div class="message-bubble markdown">${renderedContent}</div>
            ${!isUser ? `<div class="message-time">${time}</div>` : ''}
        </div>
    `;

    messagesArea.appendChild(messageDiv);
    scrollToBottom();
}

function scrollToBottom() {
    const messagesArea = document.getElementById('messagesArea');
    messagesArea.scrollTop = messagesArea.scrollHeight;
}

function showTypingIndicator() {
    hideWelcomeScreen();
    const messagesArea = document.getElementById('messagesArea');
    const typingDiv = document.createElement('div');
    typingDiv.className = 'message ai';
    typingDiv.id = 'typingIndicator';
    typingDiv.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="message-content">
            <div class="message-bubble">
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    `;
    messagesArea.appendChild(typingDiv);
    scrollToBottom();
}

function hideTypingIndicator() {
    const typingIndicator = document.getElementById('typingIndicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    if (!message) return;

    addMessageToUI(message, true, null);
    input.value = '';
    input.style.height = 'auto';

    showTypingIndicator();

    const sendBtn = document.getElementById('sendBtn');
    sendBtn.disabled = true;
    sendBtn.classList.add('sending');

    try {
        const response = await fetch(`${API_BASE}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                message: message,
                conversationId: currentConversationId
            })
        });

        // Handle 401 Unauthorized
        if (response.status === 401) {
            hideTypingIndicator();
            showToast('登录已过期，请重新登录', 'error');
            currentUsername = null;
            showAuthView();
            showLoginForm();
            return;
        }

        const result = await response.json();
        hideTypingIndicator();

        if (result.success && result.data) {
            currentConversationId = result.data.conversationId;
            saveCurrentSession();
            addMessageToUI(result.data.reply, false, null);

            // Reload sessions to update list
            await loadSessions();

            // Refresh suggestions after each message
            loadSuggestions();
        } else {
            showToast('抱歉，发生了错误，请稍后重试。', 'error');
        }
    } catch (error) {
        hideTypingIndicator();
        showToast('网络错误，请检查连接后重试。', 'error');
        console.error('Error:', error);
    } finally {
        sendBtn.disabled = false;
        sendBtn.classList.remove('sending');
    }
}

async function loadSuggestions() {
    try {
        const url = `${API_BASE}/suggestions${currentConversationId ? '?conversationId=' + encodeURIComponent(currentConversationId) : ''}`;
        const response = await fetch(url, {
            credentials: 'include'
        });
        const result = await response.json();

        if (result.success && result.data && result.data.suggestions) {
            updateQuickChips(result.data.suggestions);
        }
    } catch (error) {
        console.error('Failed to load suggestions:', error);
        // Keep default suggestions on error
    }
}

function updateQuickChips(suggestions) {
    const quickChips = document.getElementById('quickChips');
    if (!quickChips) return;

    // Clear existing chips
    quickChips.innerHTML = '';

    // Add new chips from suggestions
    suggestions.forEach(suggestion => {
        const chip = document.createElement('button');
        chip.className = 'chip';
        chip.textContent = suggestion.text;
        chip.onclick = () => sendSuggestion(suggestion.text);
        quickChips.appendChild(chip);
    });
}

function sendSuggestion(text) {
    const input = document.getElementById('messageInput');
    input.value = text;
    input.focus();

    // Auto-resize textarea to fit content
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 150) + 'px';
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    toast.className = `toast ${type} show`;
    toastMessage.textContent = message;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function formatTime(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;

    // Less than 1 minute
    if (diff < 60000) {
        return '刚刚';
    }

    // Less than 1 hour
    if (diff < 3600000) {
        return Math.floor(diff / 60000) + '分钟前';
    }

    // Less than 1 day
    if (diff < 86400000) {
        return Math.floor(diff / 3600000) + '小时前';
    }

    // Less than 7 days
    if (diff < 604800000) {
        return Math.floor(diff / 86400000) + '天前';
    }

    // Format as date
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
}
