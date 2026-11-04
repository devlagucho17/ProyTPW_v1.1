const contextPath = window.appContextPath || "";
const defaultImage = "https://images.unsplash.com/photo-1661030419605-3d4ca9cda193?auto=format&fit=crop&w=800&q=80";
let csrfToken = "";
let currentUser = null;
let auctions = [];
let selectedCategory = "Todas";
let selectedAuction = null;
let currentStep = 1;
let remainingSeconds = 0;
let countdownTimer = null;
let authMode = "login";
let previewObjectUrl = null;

const elements = id => document.getElementById(id);

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function money(value, decimals = 0) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals
    }).format(Number(value) || 0);
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.method && options.method.toUpperCase() !== "GET") {
        headers.set("X-CSRF-Token", csrfToken);
    }
    if (options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(`${contextPath}${path}`, {
        ...options,
        headers,
        credentials: "same-origin"
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.error || `Error ${response.status}`);
    return result;
}

function setUser(user) {
    currentUser = user && user.id ? user : null;
    const name = currentUser ? currentUser.displayName : "Ingresar";
    const initials = currentUser ? currentUser.displayName.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase() : "?";
    elements("headerUserName").textContent = name;
    elements("headerAvatar").textContent = initials;
    elements("accountName").textContent = currentUser ? currentUser.displayName : "Mi cuenta";
    elements("accountGreeting").textContent = currentUser ? currentUser.displayName.split(/\s+/)[0] : "invitado";
    elements("accountAvatar").textContent = initials;
    elements("logoutButton").classList.toggle("d-none", !currentUser);
}

function showPage(pageName) {
    const page = elements(`page-${pageName}`);
    if (!page) return;
    if (pageName === "cuenta" && !currentUser) {
        openAuthModal("login");
        return;
    }
    document.querySelectorAll(".app-page").forEach(item => item.classList.remove("active"));
    page.classList.add("active");
    document.querySelectorAll(".screen-switcher button").forEach(button => {
        button.classList.toggle("active", button.dataset.page === pageName);
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
    if (pageName === "cuenta") loadAccount();
}

document.querySelectorAll("[data-page]").forEach(button => {
    button.addEventListener("click", () => {
        if (button.dataset.page === "detalle" && selectedAuction) {
            openAuction(selectedAuction.id);
        } else {
            showPage(button.dataset.page);
        }
    });
});

function safeImageUrl(value) {
    if (typeof value !== "string") return defaultImage;
    if (/^\/images\/[0-9a-f-]{36}\.(jpg|png)$/i.test(value)) return `${contextPath}${value}`;
    if (/^https:\/\/images\.unsplash\.com\//i.test(value)) return value;
    return defaultImage;
}

function formatDuration(seconds) {
    const value = Math.max(0, Number(seconds) || 0);
    const hours = Math.floor(value / 3600);
    const minutes = Math.floor((value % 3600) / 60);
    const remainder = value % 60;
    if (hours > 0) return `${String(hours).padStart(2, "0")}h ${String(minutes).padStart(2, "0")}m`;
    return `${String(minutes).padStart(2, "0")}m ${String(remainder).padStart(2, "0")}s`;
}

function normalizeAuction(auction) {
    return {
        ...auction,
        price: Number(auction.currentPrice),
        startingPrice: Number(auction.startingPrice),
        increment: Number(auction.increment),
        bids: Number(auction.bidCount),
        seconds: Number(auction.secondsRemaining),
        image: safeImageUrl(auction.imageUrl),
        favorite: false
    };
}

async function loadAuctions() {
    try {
        const results = await api("/api/auctions");
        auctions = results.map(normalizeAuction);
        renderAuctions();
        if (auctions.length) {
            const featured = auctions.find(item => item.status === "live") || auctions[0];
            updateHero(featured);
            await loadAuction(featured.id);
        } else {
            elements("heroTitle").textContent = "Subastas únicas en tiempo real";
            elements("heroPrice").textContent = "—";
            elements("auctionGrid").innerHTML = '<p class="text-muted">Todavía no hay subastas. Inicia sesión para publicar la primera.</p>';
        }
    } catch (error) {
        showAuctionError(error.message);
    }
}

function updateHero(auction) {
    elements("heroTitle").textContent = auction.title;
    elements("heroPrice").textContent = money(auction.price);
    elements("heroStatus").textContent = auction.status === "live" ? "EN VIVO" : auction.status === "soon" ? "PRÓXIMAMENTE" : "FINALIZADA";
    elements("heroStatus").className = `status-badge ${auction.status === "live" ? "status-live" : "status-upcoming"}`;
    const image = document.querySelector(".hero-photo img");
    image.src = auction.image;
    image.alt = auction.title;
}

function showAuctionError(message) {
    elements("auctionGrid").innerHTML = `<p class="text-danger" role="alert">${escapeHtml(message)}</p>`;
    elements("resultCount").textContent = "Error";
}

function renderAuctions() {
    const status = elements("statusFilter").value;
    const price = elements("priceFilter").value;
    const sort = elements("sortAuctions").value;
    const search = elements("globalSearch").value.trim().toLocaleLowerCase();
    const visible = auctions.filter(item =>
        (selectedCategory === "Todas" || item.category === selectedCategory) &&
        (status === "all" || item.status === status) &&
        (price === "all" ||
            (price === "under-500" && item.price <= 500) ||
            (price === "500-2000" && item.price > 500 && item.price <= 2000) ||
            (price === "over-2000" && item.price > 2000)) &&
        (!search || `${item.title} ${item.category}`.toLocaleLowerCase().includes(search))
    );
    visible.sort(sort === "price"
        ? (a, b) => b.price - a.price
        : (a, b) => a.seconds - b.seconds);
    elements("resultCount").textContent = `${visible.length} resultados`;
    elements("auctionGrid").innerHTML = visible.length ? visible.map(item => `
        <article class="auction-card">
            <div class="auction-image">
                <img src="${escapeHtml(item.image)}" alt="${escapeHtml(item.title)}">
                <span class="status-badge ${item.status === "live" ? "status-live" : "status-upcoming"}">${item.status === "live" ? "EN VIVO" : item.status === "soon" ? "PRÓXIMAMENTE" : "FINALIZADA"}</span>
                <button class="card-favorite ${item.favorite ? "active" : ""}" data-favorite="${item.id}" aria-label="Favorito"><i class="bi bi-heart${item.favorite ? "-fill" : ""}"></i></button>
            </div>
            <div class="auction-body">
                <span class="product-category">${escapeHtml(item.category)}</span>
                <h3>${escapeHtml(item.title)}</h3>
                <div class="auction-data">
                    <span><small>${item.bids ? "PUJA ACTUAL" : "PRECIO ACTUAL"}</small><strong>${money(item.price)}</strong></span>
                    <span><small>${item.status === "soon" ? "INICIA EN" : "TERMINA EN"}</small><strong class="red-time"><i class="bi bi-clock"></i>${formatDuration(item.status === "soon" ? Math.max(0, Math.floor((new Date(item.startsAt).getTime() - Date.now()) / 1000)) : item.seconds)}</strong></span>
                </div>
                <div class="auction-footer">
                    <span>${item.bids} pujas</span>
                    <button class="secondary-button open-detail" data-open-auction="${item.id}">Ver subasta <i class="bi bi-arrow-right"></i></button>
                </div>
            </div>
        </article>`).join("") : '<p class="text-muted">No encontramos subastas con esos filtros.</p>';
    document.querySelectorAll("[data-open-auction]").forEach(button => {
        button.addEventListener("click", () => openAuction(Number(button.dataset.openAuction)));
    });
    document.querySelectorAll("[data-favorite]").forEach(button => {
        button.addEventListener("click", () => {
            const auction = auctions.find(item => item.id === Number(button.dataset.favorite));
            if (auction) auction.favorite = !auction.favorite;
            renderAuctions();
        });
    });
}

async function loadCategories() {
    try {
        const categories = await api("/api/auctions/categories");
        elements("auctionCategory").innerHTML = '<option value="">Selecciona una categoría</option>'
            + categories.map(category => `<option value="${escapeHtml(category.name)}">${escapeHtml(category.name)}</option>`).join("");
        const tabs = elements("categoryTabs");
        tabs.innerHTML = '<button class="active" data-category="Todas">Todas</button>'
            + categories.map(category => `<button data-category="${escapeHtml(category.name)}">${escapeHtml(category.name)}</button>`).join("");
        document.querySelectorAll("#categoryTabs button").forEach(button => {
            button.addEventListener("click", () => {
                selectedCategory = button.dataset.category;
                document.querySelectorAll("#categoryTabs button").forEach(item => item.classList.toggle("active", item === button));
                renderAuctions();
            });
        });
    } catch (error) {
        elements("uploadError").textContent = error.message;
    }
}

document.querySelectorAll("#categoryTabs button").forEach(button => {
    button.addEventListener("click", () => {
        selectedCategory = button.dataset.category;
        document.querySelectorAll("#categoryTabs button").forEach(item => item.classList.toggle("active", item === button));
        renderAuctions();
    });
});
["statusFilter", "priceFilter", "sortAuctions", "globalSearch"].forEach(id => {
    elements(id).addEventListener(id === "globalSearch" ? "input" : "change", renderAuctions);
});

async function openAuction(id) {
    showPage("detalle");
    try {
        const result = await api(`/api/auctions/${id}`);
        selectedAuction = normalizeAuction(result.auction);
        selectedAuctionId = selectedAuction.id;
        renderAuctionDetails(result);
    } catch (error) {
        elements("detailDescription").textContent = error.message;
    }
}

let selectedAuctionId = 0;

function renderAuctionDetails(result) {
    const auction = selectedAuction;
    elements("detailTitle").textContent = auction.title;
    elements("detailCategory").textContent = auction.category;
    elements("detailCondition").textContent = auction.condition;
    elements("detailDescription").textContent = auction.description;
    elements("detailSpecCategory").textContent = auction.category;
    elements("detailStartingPrice").textContent = money(auction.startingPrice, 2);
    elements("detailIncrement").textContent = money(auction.increment, 2);
    elements("detailSellerName").textContent = auction.sellerName;
    elements("sellerName").textContent = auction.sellerName;
    elements("sellerAvatar").textContent = auction.sellerName.trim().charAt(0).toUpperCase() || "V";
    elements("sellerRating").textContent = "Vendedor de SubastIX";
    elements("detailLot").textContent = `Lote #${auction.id}`;
    elements("currentPrice").textContent = money(auction.price, 2);
    elements("leaderText").textContent = auction.bids ? `${auction.bids} pujas registradas` : "Aún no hay pujas";
    elements("detailStatus").textContent = auction.status === "live" ? "EN VIVO" : auction.status === "soon" ? "PRÓXIMAMENTE" : "FINALIZADA";
    elements("detailStatus").className = `status-badge ${auction.status === "live" ? "status-live" : "status-upcoming"}`;
    elements("mainProductImage").src = auction.image;
    elements("mainProductImage").alt = auction.title;
    elements("imageNumber").textContent = "1";
    const thumbnails = elements("thumbnailList");
    thumbnails.replaceChildren();
    const thumbnail = document.createElement("button");
    thumbnail.className = "active";
    thumbnail.type = "button";
    const thumbnailImage = document.createElement("img");
    thumbnailImage.src = auction.image;
    thumbnailImage.alt = auction.title;
    thumbnail.appendChild(thumbnailImage);
    thumbnail.addEventListener("click", () => {
        elements("mainProductImage").src = auction.image;
        elements("imageNumber").textContent = "1";
    });
    thumbnails.appendChild(thumbnail);
    remainingSeconds = auction.status === "soon"
        ? Math.max(0, Math.floor((new Date(auction.startsAt).getTime() - Date.now()) / 1000))
        : auction.seconds;
    updateCountdownDisplay();
    renderBidHistory(result.bids || []);
    updateBidControls();
}

function updateBidControls() {
    const input = elements("bidAmount");
    const minimum = selectedAuction ? selectedAuction.price + selectedAuction.increment : 0;
    elements("minimumBid").textContent = selectedAuction ? money(minimum, 2) : "—";
    input.min = String(minimum);
    input.value = selectedAuction && selectedAuction.status === "live" ? minimum.toFixed(2) : "";
    document.querySelectorAll(".quick-offers button").forEach((button, index) => {
        const amount = minimum + [0, 100, 250][index];
        button.dataset.amount = amount.toFixed(2);
        button.textContent = money(amount);
        button.disabled = !selectedAuction || selectedAuction.status !== "live";
    });
    input.disabled = !selectedAuction || selectedAuction.status !== "live";
    elements("placeBidButton").disabled = !currentUser || !selectedAuction || selectedAuction.status !== "live";
}

document.querySelectorAll(".quick-offers button").forEach(button => {
    button.addEventListener("click", () => {
        elements("bidAmount").value = button.dataset.amount;
        elements("bidAmount").classList.remove("is-invalid");
    });
});

elements("bidAmount").addEventListener("input", event => event.target.classList.remove("is-invalid"));

elements("placeBidButton").addEventListener("click", async () => {
    if (!currentUser) {
        openAuthModal("login");
        return;
    }
    if (!selectedAuction) return;
    const input = elements("bidAmount");
    const offer = Number(input.value);
    const minimum = selectedAuction.price + selectedAuction.increment;
    if (!Number.isFinite(offer) || offer < minimum || Math.round(offer * 100) !== offer * 100) {
        input.classList.add("is-invalid");
        input.setCustomValidity(`La puja mínima es ${money(minimum, 2)}.`);
        input.reportValidity();
        return;
    }
    input.setCustomValidity("");
    try {
        const auctionId = selectedAuction.id;
        await api(`/api/auctions/${auctionId}/bids`, {
            method: "POST",
            body: JSON.stringify({ amount: offer.toFixed(2) })
        });
        await loadAuctions();
        await openAuction(auctionId);
        elements("successTitle").textContent = "¡Puja realizada!";
        elements("successMessage").textContent = `Tu oferta de ${money(offer, 2)} fue registrada.`;
        bootstrap.Modal.getOrCreateInstance(elements("successModal")).show();
        if (currentUser) loadAccount();
    } catch (error) {
        input.classList.add("is-invalid");
        input.setCustomValidity(error.message);
        input.reportValidity();
    }
});

function renderBidHistory(bids) {
    elements("bidHistoryRows").innerHTML = bids.length ? bids.slice(0, 6).map((bid, index) => `
        <div class="history-row ${index === 0 ? "leading" : ""}">
            <span>${index === 0 ? '<i class="bi bi-graph-up-arrow"></i>' : ""}${escapeHtml(bid.bidderName)}</span>
            <strong>${money(bid.amount, 2)}</strong>
            <time>${new Date(bid.createdAt).toLocaleTimeString("es-ES")}</time>
        </div>`).join("") : '<p class="text-muted small">Aún no hay pujas para esta subasta.</p>';
    elements("bidTotal").textContent = `${bids.length} pujas`;
}

function updateCountdownDisplay() {
    const hours = String(Math.floor(remainingSeconds / 3600)).padStart(2, "0");
    const minutes = String(Math.floor((remainingSeconds % 3600) / 60)).padStart(2, "0");
    const seconds = String(remainingSeconds % 60).padStart(2, "0");
    elements("detailCountdown").innerHTML = `<span><b>${hours}</b><small>HORAS</small></span><i>:</i><span><b>${minutes}</b><small>MIN</small></span><i>:</i><span><b>${seconds}</b><small>SEG</small></span>`;
    elements("heroCountdown").textContent = `${hours} : ${minutes} : ${seconds}`;
    if (remainingSeconds === 0 && selectedAuction?.status === "live") {
        selectedAuction.status = "ended";
        updateBidControls();
    }
}

function startCountdown() {
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(() => {
        if (remainingSeconds > 0) remainingSeconds--;
        updateCountdownDisplay();
    }, 1000);
}

function openAuthModal(mode = "login") {
    setAuthMode(mode);
    bootstrap.Modal.getOrCreateInstance(elements("authModal")).show();
}

function setAuthMode(mode) {
    authMode = mode;
    const registering = mode === "register";
    elements("authModalTitle").textContent = registering ? "Crear cuenta" : "Inicia sesión";
    elements("authNameField").classList.toggle("d-none", !registering);
    elements("authRoleField").classList.toggle("d-none", !registering);
    elements("authName").required = registering;
    elements("authPassword").minLength = registering ? 12 : 1;
    elements("authPassword").setAttribute("autocomplete", registering ? "new-password" : "current-password");
    elements("passwordHint").textContent = registering ? "Usa entre 12 y 128 caracteres." : "";
    elements("authSubmit").textContent = registering ? "Crear cuenta" : "Iniciar sesión";
    elements("authError").textContent = "";
}

elements("showLogin").addEventListener("click", () => setAuthMode("login"));
elements("showRegister").addEventListener("click", () => setAuthMode("register"));
elements("authButton").addEventListener("click", () => {
    if (currentUser) showPage("cuenta");
    else openAuthModal("login");
});

elements("authForm").addEventListener("submit", async event => {
    event.preventDefault();
    if (!event.currentTarget.reportValidity()) return;
    const endpoint = authMode === "register" ? "register" : "login";
    const body = {
        displayName: elements("authName").value,
        email: elements("authEmail").value,
        password: elements("authPassword").value,
        role: elements("authRole").value
    };
    try {
        const result = await api(`/api/auth/${endpoint}`, {
            method: "POST",
            body: JSON.stringify(body)
        });
        csrfToken = result.csrfToken;
        setUser(result.user);
        bootstrap.Modal.getOrCreateInstance(elements("authModal")).hide();
        updateBidControls();
        if (document.getElementById("page-cuenta").classList.contains("active")) loadAccount();
    } catch (error) {
        elements("authError").textContent = error.message;
    }
});

async function loadAccount() {
    if (!currentUser) return;
    try {
        const bids = await api("/api/account");
        const leading = bids.filter(bid => bid.leading).length;
        elements("accountBidCount").textContent = String(bids.length);
        elements("accountLeadingCount").textContent = String(leading);
        elements("accountOutbidCount").textContent = String(bids.length - leading);
        elements("accountBidRows").innerHTML = bids.length ? bids.map(bid => {
            const seconds = Math.max(0, Math.floor((new Date(bid.endsAt).getTime() - Date.now()) / 1000));
            return `<tr>
                <td><div class="table-product"><img src="${escapeHtml(safeImageUrl(bid.imageUrl))}" alt="${escapeHtml(bid.title)}"><span><b>${escapeHtml(bid.title)}</b><small>Lote #${bid.auctionId}</small></span></div></td>
                <td><b>${money(bid.myBid, 2)}</b></td><td><b>${money(bid.currentPrice, 2)}</b></td>
                <td><span class="status-badge ${bid.leading ? "status-success" : "status-danger"}">${bid.leading ? "Vas ganando" : "Superado"}</span></td>
                <td><span class="table-time"><i class="bi bi-clock"></i>${formatDuration(seconds)}</span></td>
                <td><button class="${bid.leading ? "secondary-button" : "primary-button"} account-action" data-account-auction="${bid.auctionId}">${bid.leading ? "Ver subasta" : "Aumentar puja"}</button></td>
            </tr>`;
        }).join("") : '<tr><td colspan="6" class="text-center text-muted py-4">Todavía no tienes pujas.</td></tr>';
        document.querySelectorAll("[data-account-auction]").forEach(button => {
            button.addEventListener("click", () => openAuction(Number(button.dataset.accountAuction)));
        });
    } catch (error) {
        elements("accountBidRows").innerHTML = `<tr><td colspan="6" class="text-danger">${escapeHtml(error.message)}</td></tr>`;
    }
}

elements("logoutButton").addEventListener("click", async () => {
    try {
        await api("/api/auth/logout", { method: "POST", body: "{}" });
        csrfToken = "";
        setUser(null);
        const session = await api("/api/auth");
        csrfToken = session.csrfToken;
        updateBidControls();
        showPage("catalogo");
    } catch (error) {
        elements("authError").textContent = error.message;
    }
});

function updateStep(step) {
    currentStep = step;
    document.querySelectorAll(".form-step").forEach(element => {
        element.classList.toggle("active", Number(element.dataset.formStep) === step);
    });
    document.querySelectorAll("#stepper button").forEach(button => {
        const buttonStep = Number(button.dataset.step);
        button.classList.toggle("active", buttonStep === step);
        button.classList.toggle("done", buttonStep < step);
        button.querySelector("span").innerHTML = buttonStep < step ? '<i class="bi bi-check-lg"></i>' : String(buttonStep);
    });
    elements("previousStep").classList.toggle("d-none", step === 1);
    elements("nextStep").classList.toggle("d-none", step === 3);
    elements("publishButton").classList.toggle("d-none", step !== 3);
}

function validateAuctionDates() {
    const start = elements("auctionStart");
    const end = elements("auctionEnd");
    const startTime = start.value ? new Date(start.value).getTime() : NaN;
    const endTime = end.value ? new Date(end.value).getTime() : NaN;
    start.setCustomValidity(start.value && startTime <= Date.now() ? "La fecha de inicio debe ser futura." : "");
    end.setCustomValidity(end.value && start.value && endTime <= startTime
        ? "La fecha de finalización debe ser posterior al inicio." : "");
}

function validateStep(step) {
    if (step === 3) validateAuctionDates();
    const section = document.querySelector(`[data-form-step="${step}"]`);
    const invalid = [...section.querySelectorAll("input, select, textarea")].find(field => !field.checkValidity());
    if (invalid) {
        invalid.reportValidity();
        return false;
    }
    return true;
}

document.querySelectorAll("#stepper button").forEach(button => {
    button.addEventListener("click", () => {
        const target = Number(button.dataset.step);
        for (let step = currentStep; step < target; step++) {
            if (!validateStep(step)) return;
        }
        updateStep(target);
    });
});
elements("nextStep").addEventListener("click", () => {
    if (validateStep(currentStep)) updateStep(Math.min(3, currentStep + 1));
});
elements("previousStep").addEventListener("click", () => updateStep(Math.max(1, currentStep - 1)));
["auctionStart", "auctionEnd"].forEach(id => elements(id).addEventListener("change", validateAuctionDates));

elements("auctionTitle").addEventListener("input", event => {
    elements("titleCount").textContent = `${event.target.value.length} / 80`;
    elements("previewTitle").textContent = event.target.value || "Título de tu subasta";
});
elements("startingPrice").addEventListener("input", event => {
    elements("previewPrice").textContent = money(event.target.value, 2);
});

function clearImagePreview() {
    if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
    previewObjectUrl = null;
    elements("imagePreviewList").replaceChildren();
    elements("productPreview").innerHTML = '<i class="bi bi-image"></i><span>La imagen principal aparecerá aquí</span>';
}

elements("imageInput").addEventListener("change", event => {
    const file = event.target.files[0];
    elements("uploadError").textContent = "";
    clearImagePreview();
    if (!file) return;
    if (!["image/jpeg", "image/png"].includes(file.type) || file.size > 10 * 1024 * 1024) {
        elements("uploadError").textContent = "Selecciona un archivo JPG o PNG de hasta 10 MB.";
        event.target.value = "";
        return;
    }
    previewObjectUrl = URL.createObjectURL(file);
    const thumbnail = document.createElement("img");
    thumbnail.src = previewObjectUrl;
    thumbnail.alt = "Vista previa";
    elements("imagePreviewList").appendChild(thumbnail);
    const mainImage = document.createElement("img");
    mainImage.src = previewObjectUrl;
    mainImage.alt = "Vista previa del producto";
    elements("productPreview").replaceChildren(mainImage);
});

const uploadZone = elements("uploadZone");
uploadZone.addEventListener("dragover", event => event.preventDefault());
uploadZone.addEventListener("drop", event => {
    event.preventDefault();
    const fileInput = elements("imageInput");
    if (event.dataTransfer.files.length) {
        const transfer = new DataTransfer();
        transfer.items.add(event.dataTransfer.files[0]);
        fileInput.files = transfer.files;
        fileInput.dispatchEvent(new Event("change", { bubbles: true }));
    }
});

elements("auctionForm").addEventListener("submit", async event => {
    event.preventDefault();
    validateAuctionDates();
    if (!validateStep(3) || !validateStep(1)) return;
    if (!currentUser) {
        openAuthModal("login");
        return;
    }
    const form = new FormData();
    form.append("title", elements("auctionTitle").value.trim());
    form.append("category", elements("auctionCategory").value);
    form.append("condition", elements("auctionCondition").value);
    form.append("description", elements("auctionDescription").value.trim());
    form.append("startingPrice", elements("startingPrice").value);
    form.append("increment", elements("bidIncrementInput").value);
    form.append("startsAt", new Date(elements("auctionStart").value).toISOString());
    form.append("endsAt", new Date(elements("auctionEnd").value).toISOString());
    const image = elements("imageInput").files[0];
    if (image) form.append("image", image);
    try {
        const result = await api("/api/auctions", { method: "POST", body: form });
        bootstrap.Modal.getOrCreateInstance(elements("successModal")).show();
        elements("successTitle").textContent = "Subasta publicada";
        elements("successMessage").textContent = `La subasta “${result.auction.title}” se guardó correctamente.`;
        event.currentTarget.reset();
        clearImagePreview();
        elements("titleCount").textContent = "0 / 80";
        elements("previewTitle").textContent = "Título de tu subasta";
        elements("previewPrice").textContent = "$0.00";
        updateStep(1);
        await loadAuctions();
    } catch (error) {
        elements("uploadError").textContent = error.message;
    }
});

async function initialize() {
    try {
        const session = await api("/api/auth");
        csrfToken = session.csrfToken;
        setUser(session.user);
        await loadCategories();
        await loadAuctions();
    } catch (error) {
        showAuctionError(error.message);
    }
    startCountdown();
}

elements("logoutButton").classList.add("d-none");
initialize();
