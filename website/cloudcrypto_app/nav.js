
// Common Navigation Bar
const navbarHTML = `
    <nav class="navbar navbar-expand-lg navbar-light fixed-top">
        <div class="container">
            <a class="navbar-brand d-flex align-items-center" href="index.html">
                <img src="MobileCloudCrypto_1024.png" alt="Cloud Crypto Logo" style="height: 40px; width: auto; margin-right: 10px;">
                <span style="background: linear-gradient(135deg, var(--primary-cyan), var(--primary-purple)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">Cloud Crypto</span>
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item"><a class="nav-link" href="index.html">Home</a></li>
                    <li class="nav-item"><a class="nav-link" href="wallet.html">Wallet</a></li>
                    <li class="nav-item"><a class="nav-link" href="index.html#features">Features</a></li>
                    <li class="nav-item"><a class="nav-link" href="index.html#stats">Stats</a></li>
                    <li class="nav-item"><a class="nav-link" href="index.html#devices">Devices</a></li>
                    <li class="nav-item"><a class="nav-link" href="support.html">Support</a></li>
                    <li class="nav-item"><a class="nav-link" href="privacy_policy.html">Privacy</a></li>
                </ul>
            </div>
        </div>
    </nav>
`;

document.addEventListener('DOMContentLoaded', function() {
    const navContainer = document.getElementById('navbar-container');
    if (navContainer) {
        navContainer.innerHTML = navbarHTML;
        
        // Highlight active link
        const currentPath = window.location.pathname.split('/').pop() || 'index.html';
        document.querySelectorAll('.nav-link').forEach(link => {
            const href = link.getAttribute('href');
            // Check for exact match or anchor match on same page
            if (href === currentPath || (currentPath === 'index.html' && href === 'index.html')) {
                link.classList.add('active');
            }
        });

        // Navbar Scroll Effect
        window.addEventListener('scroll', function() {
            const navbar = document.querySelector('.navbar');
            if (navbar) {
                if (window.scrollY > 50) {
                    navbar.style.background = 'rgba(255, 255, 255, 0.98)';
                } else {
                    navbar.style.background = 'rgba(255, 255, 255, 0.95)';
                }
            }
        });
    }
});
