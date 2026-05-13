class MatchCard extends HTMLElement {
    connectedCallback() {
        // Lấy dữ liệu từ attribute
        const tournament = this.getAttribute('tournament') || 'FIFA World Cup 2026™';
        const date = this.getAttribute('date') || 'Jun 12, 02:00';
        const team1 = this.getAttribute('team1') || 'Mexico';
        const flag1 = this.getAttribute('flag1') || 'mx';
        const team2 = this.getAttribute('team2') || 'South Africa';
        const flag2 = this.getAttribute('flag2') || 'za';
        const group = this.getAttribute('group') || 'Group Stage · Group A';

        // Render HTML thuần túy, không style inline
        this.innerHTML = `
            <div class="match-card">
                <div class="header">
                    <span class="tournament">${tournament}</span>
                    <span class="separator">·</span>
                    <span class="date">${date}</span>
                </div>

                <div class="body">
                    <div class="team">
                        <div class="flag-container">
                            <img src="https://flagcdn.com/w160/${flag1.toLowerCase()}.png" 
                                 alt="${team1}"
                                 onerror="this.src='https://flagcdn.com/w160/un.png'">
                        </div>
                        <span class="team-name">${team1}</span>
                    </div>

                    <div class="vs-divider">vs</div>

                    <div class="team">
                        <div class="flag-container">
                            <img src="https://flagcdn.com/w160/${flag2.toLowerCase()}.png" 
                                 alt="${team2}"
                                 onerror="this.src='https://flagcdn.com/w160/un.png'">
                        </div>
                        <span class="team-name">${team2}</span>
                    </div>
                </div>

                <div class="footer">
                    ${group}
                </div>
            </div>
        `;
    }
}

// Đăng ký component (tránh lỗi duplicate)
if (!customElements.get('match-info')) {
    customElements.define('match-info', MatchCard);
}