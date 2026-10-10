(function () {
    const panel = document.getElementById("appPanelNavegacion");
    const superposicion = document.getElementById(
        "appPanelSuperposicion"
    );

    if (!panel || !superposicion) {
        return;
    }

    let temporizadorCierre;

    function esVistaMovil() {
        return window.matchMedia("(max-width: 760px)").matches;
    }

    function cambiarEstado(abierto) {
        const movil = esVistaMovil();

        panel.classList.toggle("abierto", abierto);
        document.body.classList.toggle(
            "app-panel-expandido",
            abierto && !movil
        );
        document.body.classList.toggle(
            "app-panel-movil-abierto",
            abierto && movil
        );
        superposicion.classList.toggle(
            "visible",
            abierto && movil
        );
        panel.setAttribute("aria-expanded", String(abierto));
        superposicion.setAttribute(
            "aria-hidden",
            String(!(abierto && movil))
        );
    }

    function marcarOpcionActual() {
        const ruta = window.location.pathname.replace(/\/$/, "") || "/";
        const enlaces = panel.querySelectorAll("a[data-ruta], a[data-ruta-exacta]");
        let mejorCoincidencia = null;
        let largoCoincidencia = -1;

        enlaces.forEach(function (enlace) {
            const rutaExacta = enlace.dataset.rutaExacta;
            const rutaBase = enlace.dataset.ruta;
            const coincide = rutaExacta
                ? ruta === rutaExacta
                : ruta === rutaBase || ruta.startsWith(rutaBase + "/");
            const largo = (rutaExacta || rutaBase).length;

            if (coincide && largo > largoCoincidencia) {
                mejorCoincidencia = enlace;
                largoCoincidencia = largo;
            }
        });

        if (mejorCoincidencia) {
            mejorCoincidencia.classList.add("activo");
            mejorCoincidencia.setAttribute("aria-current", "page");
        }
    }

    superposicion.addEventListener("click", function () {
        cambiarEstado(false);
    });

    panel.addEventListener("pointerenter", function () {
        if (esVistaMovil()) {
            return;
        }

        window.clearTimeout(temporizadorCierre);
        cambiarEstado(true);
    });

    panel.addEventListener("pointerleave", function () {
        if (esVistaMovil()) {
            return;
        }

        temporizadorCierre = window.setTimeout(function () {
            cambiarEstado(false);
        }, 180);
    });

    panel.addEventListener("click", function () {
        if (!panel.classList.contains("abierto")) {
            cambiarEstado(true);
        }
    });

    panel.addEventListener("focusin", function () {
        cambiarEstado(true);
    });

    document.addEventListener("keydown", function (evento) {
        if (evento.key === "Escape" && panel.classList.contains("abierto")) {
            cambiarEstado(false);
        }
    });

    window.addEventListener("resize", function () {
        cambiarEstado(false);
    });

    marcarOpcionActual();
    cambiarEstado(false);
}());
