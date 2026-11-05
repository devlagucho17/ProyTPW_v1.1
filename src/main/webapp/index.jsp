<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SubastIX | Subastas en tiempo real</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Mono:wght@700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/subastix.css" rel="stylesheet">
</head>
<body>
    <header class="main-header">
        <div class="container-fluid header-inner">
            <button class="brand border-0 bg-transparent" data-page="catalogo">
                <span class="brand-mark"><i class="bi bi-hammer"></i></span>
                <span>Subast<span>IX</span></span>
            </button>

            <nav class="desktop-nav">
                <button data-page="catalogo">Explorar</button>
                <button data-page="publicar">Vender</button>
                <button data-page="cuenta">Mis pujas</button>
            </nav>

            <div class="header-search">
                <i class="bi bi-search"></i>
                <input id="globalSearch" type="search" placeholder="Buscar subastas, marcas o categorías">
                <kbd>⌘ K</kbd>
            </div>

            <div class="header-actions">
                <div class="dropdown">
                    <button class="icon-button" data-bs-toggle="dropdown" aria-label="Notificaciones">
                        <i class="bi bi-bell"></i>
                    </button>
                    <div class="dropdown-menu dropdown-menu-end notification-menu">
                        <strong>Notificaciones</strong>
                        <div><small>No tienes notificaciones nuevas.</small></div>
                    </div>
                </div>
                <button class="user-button" id="authButton" type="button">
                    <span class="avatar" id="headerAvatar">?</span><span id="headerUserName">Ingresar</span>
                </button>
                <button class="mobile-menu-button" data-bs-toggle="offcanvas" data-bs-target="#mobileMenu">
                    <i class="bi bi-list"></i>
                </button>
            </div>
        </div>
    </header>

    <div class="offcanvas offcanvas-end" tabindex="-1" id="mobileMenu">
        <div class="offcanvas-header">
            <span class="brand"><span class="brand-mark"><i class="bi bi-hammer"></i></span><span>Subast<span>IX</span></span></span>
            <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
        </div>
        <div class="offcanvas-body mobile-navigation">
            <button data-page="catalogo" data-bs-dismiss="offcanvas"><i class="bi bi-grid"></i> Explorar</button>
            <button data-page="detalle" data-bs-dismiss="offcanvas"><i class="bi bi-broadcast"></i> Subasta en vivo</button>
            <button data-page="cuenta" data-bs-dismiss="offcanvas"><i class="bi bi-person"></i> Mi cuenta</button>
            <button data-page="publicar" data-bs-dismiss="offcanvas"><i class="bi bi-plus-circle"></i> Publicar producto</button>
        </div>
    </div>

    <!-- PANTALLA 1: CATÁLOGO -->
    <main id="page-catalogo" class="app-page active">
        <section class="hero-section">
            <div class="hero-copy">
                <div class="d-flex align-items-center gap-3">
                    <span class="status-badge status-upcoming" id="heroStatus">SIN SUBASTAS</span>
                    <span class="eyebrow text-white-50">SUBASTAS DESTACADAS</span>
                </div>
                <h1 id="heroTitle">Subastas únicas<br>en tiempo real</h1>
                <p>Descubre artículos únicos y participa en subastas en tiempo real.</p>
                <div class="hero-data">
                    <span><small>Puja actual</small><strong id="heroPrice">—</strong></span>
                    <span><small>Tiempo restante</small><strong class="digital" id="heroCountdown">00 : 00 : 00</strong></span>
                </div>
                <button class="primary-button" data-page="detalle">Pujar ahora <i class="bi bi-arrow-right"></i></button>
            </div>
            <div class="hero-photo">
                <img src="https://images.unsplash.com/photo-1661030419605-3d4ca9cda193?auto=format&fit=crop&w=1200&q=85" alt="Artículo destacado de subasta">
                <span class="verified-label"><i class="bi bi-broadcast"></i> Oportunidades en vivo</span>
            </div>
        </section>

        <section class="content-shell catalog-content">
            <div class="section-title">
                <div>
                    <span class="eyebrow text-blue">OPORTUNIDADES EN TIEMPO REAL</span>
                    <h2>Explorar subastas</h2>
                    <p>Descubre artículos únicos seleccionados por vendedores verificados.</p>
                </div>
                <span id="resultCount">Cargando subastas…</span>
            </div>

            <div class="filter-panel">
                <div class="category-tabs" id="categoryTabs">
                    <button class="active" data-category="Todas">Todas</button>
                    <button data-category="Tecnología y Laptops">Tecnología y Laptops</button>
                    <button data-category="Consolas y Videojuegos">Consolas y Videojuegos</button>
                    <button data-category="Coleccionables">Coleccionables</button>
                </div>
                <div class="filter-selects">
                    <label><small>Estado</small><select id="statusFilter"><option value="all">Todos</option><option value="live">En vivo</option><option value="soon">Próximamente</option></select></label>
                    <label><small>Precio</small><select id="priceFilter"><option value="all">Cualquier precio</option><option value="under-500">Hasta $500</option><option value="500-2000">$500 — $2,000</option><option value="over-2000">Más de $2,000</option></select></label>
                    <label><small>Ordenar por</small><select id="sortAuctions"><option value="time">Termina pronto</option><option value="price">Mayor precio</option></select></label>
                </div>
            </div>

            <div class="auction-grid" id="auctionGrid"></div>
        </section>
    </main>

    <!-- PANTALLA 2: DETALLE -->
    <main id="page-detalle" class="app-page">
        <div class="content-shell detail-content">
            <nav class="breadcrumb-line"><button data-page="catalogo">Subastas</button><span>/</span><button id="detailBreadcrumbCategory">—</button><span>/</span><b id="detailBreadcrumbTitle">Selecciona una subasta</b></nav>
            <div class="live-strip"><span><i></i> <span id="liveStripStatus">ESTADO DE SUBASTA</span></span><small>Las pujas se actualizan desde la base de datos</small></div>

            <div class="detail-layout">
                <section>
                    <div class="main-gallery">
                        <img id="mainProductImage" src="https://images.unsplash.com/photo-1661030419605-3d4ca9cda193?auto=format&fit=crop&w=1200&q=85" alt="Artículo de subasta">
                        <button class="favorite-button"><i class="bi bi-heart"></i></button>
                        <span class="image-counter"><i class="bi bi-images"></i> <span id="imageNumber">1</span> / 4</span>
                    </div>
                    <div class="thumbnail-list" id="thumbnailList">
                        <button class="active" data-image="https://images.unsplash.com/photo-1661030419605-3d4ca9cda193?auto=format&fit=crop&w=1200&q=85"><img src="https://images.unsplash.com/photo-1661030419605-3d4ca9cda193?auto=format&fit=crop&w=200&q=75" alt="Vista 1"></button>
                        <button data-image="https://images.unsplash.com/photo-1628911774602-74a0cfee9b0d?auto=format&fit=crop&w=1200&q=85"><img src="https://images.unsplash.com/photo-1628911774602-74a0cfee9b0d?auto=format&fit=crop&w=200&q=75" alt="Vista 2"></button>
                        <button data-image="https://images.unsplash.com/photo-1678081624937-bc4df29b5856?auto=format&fit=crop&w=1200&q=85"><img src="https://images.unsplash.com/photo-1678081624937-bc4df29b5856?auto=format&fit=crop&w=200&q=75" alt="Vista 3"></button>
                        <button data-image="https://images.unsplash.com/photo-1622086634181-3f944f83a49e?auto=format&fit=crop&w=1200&q=85"><img src="https://images.unsplash.com/photo-1622086634181-3f944f83a49e?auto=format&fit=crop&w=200&q=75" alt="Vista 4"></button>
                    </div>

                    <article class="product-information">
                        <div class="product-heading">
                            <div><span class="product-category" id="detailCategory">SUBASTA</span><h1 id="detailTitle">Selecciona una subasta</h1></div>
                            <span class="status-badge status-success" id="detailCondition">—</span>
                        </div>
                        <ul class="nav info-tabs">
                            <li><button class="active">Descripción</button></li>
                            <li><button>Detalles</button></li>
                            <li><button>Envío</button></li>
                        </ul>
                        <p id="detailDescription">Elige un artículo del catálogo para consultar sus detalles.</p>
                        <div class="specification-grid">
                            <span><small>Categoría</small><b id="detailSpecCategory">—</b></span>
                            <span><small>Precio inicial</small><b id="detailStartingPrice">—</b></span>
                            <span><small>Incremento</small><b id="detailIncrement">—</b></span>
                            <span><small>Vendedor</small><b id="detailSellerName">—</b></span>
                        </div>
                        <div class="seller-line">
                            <span class="seller-avatar" id="sellerAvatar">V</span>
                            <div><small>VENDEDOR</small><strong id="sellerName">Vendedor</strong><span id="sellerRating">Usuario de SubastIX</span></div>
                            <button class="text-button">Ver perfil</button>
                        </div>
                    </article>
                </section>

                <aside class="bid-panel">
                    <div class="d-flex justify-content-between align-items-center"><span class="status-badge status-upcoming" id="detailStatus">SIN SUBASTA</span><small id="detailLot">—</small></div>
                    <p class="countdown-label" id="countdownLabel">TIEMPO RESTANTE</p>
                    <div class="large-countdown" id="detailCountdown">
                        <span><b>00</b><small>HORAS</small></span><i>:</i>
                        <span><b>00</b><small>MIN</small></span><i>:</i>
                        <span><b>00</b><small>SEG</small></span>
                    </div>
                    <div class="current-price">
                        <small>PRECIO ACTUAL</small>
                        <strong id="currentPrice">—</strong>
                        <span><i></i> <span id="leaderText">Precio actual de la subasta</span></span>
                    </div>
                    <div class="offer-box">
                        <div class="offer-title"><b>Tu oferta</b><span>Puja mínima: <strong id="minimumBid">—</strong></span></div>
                        <div class="money-input"><span>$</span><input id="bidAmount" type="number" step="0.01"><small>USD</small></div>
                        <div class="quick-offers">
                            <button data-amount="8500">$8,500</button>
                            <button data-amount="8600">$8,600</button>
                            <button data-amount="8750">$8,750</button>
                        </div>
                        <button id="placeBidButton" class="bid-button">REALIZAR PUJA <i class="bi bi-hammer"></i></button>
                        <small class="secure-text"><i class="bi bi-shield-check"></i> Tu oferta quedará registrada en el historial</small>
                    </div>
                    <div class="extension-alert"><i class="bi bi-info-circle"></i><p><b>Extensión automática</b>Si entra una puja en el último minuto, el tiempo se extenderá 2 minutos.</p></div>
                    <div class="bid-history">
                        <div class="history-title"><b>Historial de pujas</b><span id="bidTotal">0 pujas</span></div>
                        <div class="history-header"><span>USUARIO</span><span>MONTO</span><span>HORA</span></div>
                        <div id="bidHistoryRows"></div>
                        <button class="full-text-button">Ver historial completo</button>
                    </div>
                </aside>
            </div>
        </div>
    </main>

    <!-- PANTALLA 3: MI CUENTA -->
    <main id="page-cuenta" class="app-page account-page">
        <aside class="account-sidebar">
            <div class="account-profile"><span class="avatar avatar-large" id="accountAvatar">?</span><div><b id="accountName">Mi cuenta</b><small>Cuenta SubastIX</small></div></div>
            <nav>
                <button class="active"><i class="bi bi-hammer"></i>Mis pujas <span id="accountSidebarBidCount">0</span></button>
            </nav>
            <div class="help-card"><i class="bi bi-shield-check"></i><b>¿Necesitas ayuda?</b><p>Nuestro equipo está disponible para ayudarte.</p><button>Centro de ayuda <i class="bi bi-arrow-right"></i></button></div>
        </aside>

        <section class="account-content">
            <div class="account-heading">
                <div><span class="eyebrow text-blue">MI CUENTA</span><h1>Hola, <span id="accountGreeting">invitado</span></h1><p>Gestiona tus pujas y revisa tu actividad reciente.</p></div>
                <div class="d-flex gap-2"><button class="secondary-button d-none" id="logoutButton" type="button">Cerrar sesión</button><button class="primary-button" data-page="catalogo"><i class="bi bi-plus-lg"></i> Explorar subastas</button></div>
            </div>
            <div class="summary-grid">
                <article><span class="summary-icon blue-bg"><i class="bi bi-hammer"></i></span><div><small>PUJAS REALIZADAS</small><strong id="accountBidCount">0</strong><p>Actividad registrada</p></div></article>
                <article><span class="summary-icon violet-bg"><i class="bi bi-trophy"></i></span><div><small>VAS GANANDO</small><strong id="accountLeadingCount">0</strong><p>Según la puja más alta actual</p></div></article>
                <article><span class="summary-icon green-bg"><i class="bi bi-arrow-up-right"></i></span><div><small>PUJAS SUPERADAS</small><strong id="accountOutbidCount">0</strong><p>Comprueba el estado antes de pujar</p></div></article>
            </div>
            <div class="account-table-card">
                <div class="table-card-heading"><div><h2>Historial de pujas</h2><p>Subastas en las que has participado.</p></div></div>
                <div class="table-responsive">
                    <table class="table align-middle mb-0 auction-table">
                        <thead><tr><th>PRODUCTO</th><th>MI OFERTA</th><th>OFERTA ACTUAL</th><th>ESTADO</th><th>TIEMPO RESTANTE</th><th></th></tr></thead>
                        <tbody id="accountBidRows"></tbody>
                    </table>
                </div>
            </div>
            <div class="notification-banner"><span><i class="bi bi-bell"></i></span><div><b>Mantente al día</b><p>Activa las notificaciones para saber cuándo te superan en una puja.</p></div><button class="secondary-button">Configurar alertas</button></div>
        </section>
    </main>

    <!-- PANTALLA 4: PUBLICAR -->
    <main id="page-publicar" class="app-page publish-page">
        <div class="content-shell publish-heading">
            <div><button data-page="cuenta" class="back-button"><i class="bi bi-arrow-left"></i> Volver a mis productos</button><h1>Publicar nueva subasta</h1><p>Completa la información para poner tu producto a subasta.</p></div>
            <span id="draftStatus"><i></i> Sin guardar</span>
        </div>
        <div class="stepper-background">
            <div class="content-shell stepper" id="stepper">
                <button class="active" data-step="1"><span>1</span><div><b>Información básica</b><small>Describe tu producto</small></div><i></i></button>
                <button data-step="2"><span>2</span><div><b>Multimedia</b><small>Añade imágenes de calidad</small></div><i></i></button>
                <button data-step="3"><span>3</span><div><b>Precio y fechas</b><small>Configura la subasta</small></div></button>
            </div>
        </div>

        <div class="content-shell publish-layout">
            <section class="publish-form-card">
                <form id="auctionForm">
                    <div class="form-step active" data-form-step="1">
                        <div class="form-title"><span><i class="bi bi-box-seam"></i></span><div><small>PASO 1 DE 3</small><h2>Información básica</h2><p>Cuéntanos qué hace especial a tu producto.</p></div></div>
                        <div class="row g-3">
                            <div class="col-12"><label for="auctionTitle" class="form-label">Título de la subasta</label><input id="auctionTitle" class="form-control" maxlength="80" placeholder="Ej. Rolex Datejust 41 Oyster Perpetual" required><div class="form-hint"><span>Sé claro y específico</span><span id="titleCount">0 / 80</span></div></div>
                            <div class="col-md-6"><label for="auctionCategory" class="form-label">Categoría</label><select id="auctionCategory" class="form-select" required><option value="">Selecciona una categoría</option></select></div>
                            <div class="col-md-6"><label for="auctionCondition" class="form-label">Estado del producto</label><select id="auctionCondition" class="form-select" required><option value="">Selecciona el estado</option><option>Nuevo</option><option>Usado · Excelente</option><option>Usado · Bueno</option></select></div>
                            <div class="col-12"><label for="auctionDescription" class="form-label">Descripción</label><textarea id="auctionDescription" class="form-control" rows="7" maxlength="2000" placeholder="Describe el estado, características, historia y todo lo que el comprador debe saber..." required></textarea></div>
                        </div>
                        <div class="form-tip"><i class="bi bi-lightbulb"></i><p><b>Consejo para vender mejor</b>Las descripciones detalladas generan hasta un 35% más de pujas. Incluye medidas, materiales, año y cualquier detalle de autenticidad.</p></div>
                    </div>

                    <div class="form-step" data-form-step="2">
                        <div class="form-title"><span><i class="bi bi-images"></i></span><div><small>PASO 2 DE 3</small><h2>Multimedia</h2><p>Muestra tu producto desde todos sus ángulos.</p></div></div>
                        <label class="upload-zone" id="uploadZone">
                            <input id="imageInput" name="image" type="file" accept="image/jpeg,image/png" hidden>
                            <span><i class="bi bi-cloud-arrow-up"></i></span>
                            <h3>Arrastra tus imágenes aquí</h3>
                            <p>o haz clic para buscar archivos en tu equipo</p>
                            <b class="secondary-button">Seleccionar imágenes</b>
                            <small>JPG o PNG · Máx. 10 MB</small>
                        </label>
                        <small id="uploadError" class="text-danger d-block mt-2" role="alert" aria-live="polite"></small>
                        <div id="imagePreviewList" class="image-preview-list"></div>
                        <div class="upload-advice">
                            <div><i class="bi bi-check-circle"></i><span><b>Buena iluminación</b><small>Usa luz natural y evita reflejos.</small></span></div>
                            <div><i class="bi bi-check-circle"></i><span><b>Todos los ángulos</b><small>Incluye detalles y posibles defectos.</small></span></div>
                            <div><i class="bi bi-check-circle"></i><span><b>Fondo neutro</b><small>Haz que tu producto destaque.</small></span></div>
                        </div>
                    </div>

                    <div class="form-step" data-form-step="3">
                        <div class="form-title"><span><i class="bi bi-wallet2"></i></span><div><small>PASO 3 DE 3</small><h2>Precio y fechas</h2><p>Define las condiciones financieras de tu subasta.</p></div></div>
                        <div class="row g-3">
                            <div class="col-md-6"><label for="startingPrice" class="form-label">Precio base de salida</label><div class="input-group"><span class="input-group-text">$</span><input id="startingPrice" class="form-control" type="number" min="1" step="0.01" placeholder="0.00" required></div></div>
                            <div class="col-md-6"><label for="bidIncrementInput" class="form-label">Incremento mínimo por puja</label><div class="input-group"><span class="input-group-text">$</span><input id="bidIncrementInput" class="form-control" type="number" min="1" step="0.01" value="50" required></div></div>
                            <div class="col-md-6"><label for="auctionStart" class="form-label">Fecha y hora de inicio</label><input id="auctionStart" class="form-control" type="datetime-local" required></div>
                            <div class="col-md-6"><label for="auctionEnd" class="form-label">Fecha y hora de finalización</label><input id="auctionEnd" class="form-control" type="datetime-local" required></div>
                        </div>
                        <div class="commission-box"><div><span><i class="bi bi-info-circle"></i> Pagos y comisiones</span><b>No configurados</b></div><p>Esta versión registra subastas y pujas; todavía no procesa pagos ni cobra comisiones.</p></div>
                        <label class="terms-line"><input type="checkbox" required> Confirmo que la información es correcta y acepto los <a href="#">términos para vendedores</a>.</label>
                    </div>

                    <div class="form-actions">
                        <button type="button" class="text-button" id="saveDraftButton">Guardar borrador</button>
                        <div><button type="button" id="previousStep" class="secondary-button d-none">Anterior</button><button type="button" id="nextStep" class="primary-button">Continuar <i class="bi bi-arrow-right"></i></button><button type="submit" id="publishButton" class="primary-button d-none">Publicar subasta <i class="bi bi-hammer"></i></button></div>
                    </div>
                </form>
            </section>

            <aside class="publish-aside">
                <div class="preview-card">
                    <p>VISTA PREVIA</p>
                    <div class="preview-image" id="productPreview"><i class="bi bi-image"></i><span>La imagen principal aparecerá aquí</span></div>
                    <div class="preview-info"><span class="status-badge status-upcoming">PRÓXIMAMENTE</span><h3 id="previewTitle">Título de tu subasta</h3><small>Precio inicial</small><strong id="previewPrice">$0.00</strong></div>
                </div>
                <div class="seller-protection"><i class="bi bi-shield-check"></i><div><b>Actividad registrada</b><p>Las subastas y sus pujas quedan asociadas a tu cuenta.</p><button data-page="cuenta">Ver mi cuenta</button></div></div>
                <div class="support-card"><b>¿Tienes dudas?</b><p>Nuestro equipo puede revisar tu publicación antes de activarla.</p><button>Contactar soporte <i class="bi bi-arrow-right"></i></button></div>
            </aside>
        </div>
    </main>

    <footer>
        <button class="brand border-0 bg-transparent" data-page="catalogo"><span class="brand-mark"><i class="bi bi-hammer"></i></span><span>Subast<span>IX</span></span></button>
        <p>© <span id="copyrightYear">2026</span> SubastIX. Compra y vende con confianza.</p>
        <span>Subastas y pujas registradas</span>
    </footer>

    <nav class="screen-switcher" aria-label="Selector de pantallas">
        <button class="active" data-page="catalogo"><small>01</small>Explorar</button>
        <button data-page="detalle"><small>02</small>Subasta en vivo</button>
        <button data-page="cuenta"><small>03</small>Mi cuenta</button>
        <button data-page="publicar"><small>04</small>Publicar</button>
    </nav>

    <div class="modal fade" id="successModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content success-modal">
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                <span class="success-symbol"><i class="bi bi-check-lg"></i></span>
                <h2 id="successTitle">¡Puja realizada!</h2>
                <p id="successMessage">Tu oferta se ha registrado correctamente.</p>
                <button class="primary-button w-100" data-bs-dismiss="modal">Continuar</button>
            </div>
        </div>
    </div>

    <div class="modal fade" id="authModal" tabindex="-1" aria-labelledby="authModalTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content success-modal text-start">
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                <h2 id="authModalTitle">Inicia sesión</h2>
                <div class="d-flex gap-2 mb-3">
                    <button type="button" class="secondary-button" id="showLogin">Iniciar sesión</button>
                    <button type="button" class="secondary-button" id="showRegister">Crear cuenta</button>
                </div>
                <form id="authForm">
                    <div class="mb-3 d-none" id="authNameField">
                        <label for="authName" class="form-label">Nombre</label>
                        <input id="authName" class="form-control" maxlength="80" autocomplete="name">
                    </div>
                    <div class="mb-3 d-none" id="authRoleField">
                        <label for="authRole" class="form-label">Tipo de cuenta</label>
                        <select id="authRole" class="form-select">
                            <option value="COMPRADOR">Comprador</option>
                            <option value="VENDEDOR">Vendedor</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="authEmail" class="form-label">Correo electrónico</label>
                        <input id="authEmail" class="form-control" type="email" maxlength="254" autocomplete="email" required>
                    </div>
                    <div class="mb-3">
                        <label for="authPassword" class="form-label">Contraseña</label>
                        <input id="authPassword" class="form-control" type="password" minlength="12" maxlength="128" autocomplete="current-password" required>
                        <small class="text-muted" id="passwordHint"></small>
                    </div>
                    <p class="text-danger small" id="authError" role="alert" aria-live="polite"></p>
                    <button class="primary-button w-100" type="submit" id="authSubmit">Iniciar sesión</button>
                </form>
            </div>
        </div>
    </div>

    <script>window.appContextPath = "${pageContext.request.contextPath}";</script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/subastix.js"></script>
</body>
</html>
