package cl.smid.auth.dominio.excepcion;

/**
 * Catalogo de codigos de error estables del servicio de Identidad (prefijo AUTZ-).
 * Forman parte del contrato publico: el frontend y los servicios consumidores
 * conmutan sobre estos codigos, no sobre el texto del mensaje. Por eso son
 * estables aunque el 'mensaje' cambie de redaccion o idioma.
 */
public enum CodigoError {

    /** Login fallido por cualquier causa (anti-enumeracion: causa indistinguible). */
    CREDENCIALES_INVALIDAS("AUTZ-001", "Credenciales invalidas. Verifique sus datos de acceso."),

    /** Token de refresco ausente, vencido, ya rotado, revocado o desconocido. */
    REFRESH_INVALIDO("AUTZ-002", "La sesion no puede renovarse. Inicie sesion nuevamente."),

    /** Falta el token, su firma/emisor/audiencia no validan, o esta vencido. */
    NO_AUTENTICADO("AUTZ-003", "No autenticado."),

    /** Autenticado, pero sin el rol o alcance requerido para la operacion. */
    ACCESO_DENEGADO("AUTZ-004", "No tiene autorizacion para realizar esta operacion."),

    /** El cuerpo de la peticion no supera las validaciones declarativas. */
    VALIDACION("AUTZ-005", "La solicitud contiene datos invalidos."),

    /** Recurso inexistente o fuera del alcance territorial del solicitante (-> 404). */
    RECURSO_NO_ENCONTRADO("AUTZ-404", "El recurso solicitado no existe."),

    /** Fallo no controlado: nunca filtra detalles internos al cliente. */
    ERROR_INTERNO("AUTZ-500", "Ocurrio un error interno. Intente nuevamente mas tarde.");

    private final String codigo;
    private final String mensajePorDefecto;

    CodigoError(String codigo, String mensajePorDefecto) {
        this.codigo = codigo;
        this.mensajePorDefecto = mensajePorDefecto;
    }

    public String codigo() { return codigo; }
    public String mensajePorDefecto() { return mensajePorDefecto; }
}
