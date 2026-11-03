package com.example.bymevpn.data.api

/** Short user-facing explanations for account actions, without exposing API internals. */
object AccountActionErrorMessages {
    fun googleSignIn(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        return when {
            apiError?.code == "USER_CANCELLED" -> ""
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения. Проверьте интернет и попробуйте войти снова."
            } else {
                "No connection. Check your internet and try signing in again."
            }
            apiError?.httpStatusCode == 503 || apiError?.code == "GOOGLE_AUTH_UNAVAILABLE" -> if (isRussian) {
                "Вход через Google временно недоступен. Попробуйте позже."
            } else {
                "Google sign-in is temporarily unavailable. Please try again later."
            }
            apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Google не подтвердил аккаунт. Выберите аккаунт Google и попробуйте снова."
            } else {
                "Google could not verify the account. Choose a Google account and try again."
            }
            else -> if (isRussian) {
                "Не удалось войти через Google. Попробуйте ещё раз или войдите по email."
            } else {
                "Could not sign in with Google. Try again or sign in with email."
            }
        }
    }

    fun deleteDevice(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        return when {
            apiError?.code == "UNAUTHORIZED" || apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Сеанс завершился. Войдите в аккаунт и повторите удаление."
            } else {
                "Your session expired. Sign in again and retry."
            }
            apiError?.httpStatusCode == 404 -> if (isRussian) {
                "Устройство уже удалено или больше не найдено. Обновите список."
            } else {
                "This device was already removed or could not be found. Refresh the list."
            }
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения с сервером. Проверьте интернет и попробуйте снова."
            } else {
                "Could not reach the server. Check your internet and try again."
            }
            apiError?.httpStatusCode?.let { it in 500..599 } == true -> if (isRussian) {
                "Сервер не смог удалить устройство. Попробуйте ещё раз позже."
            } else {
                "The server could not remove this device. Please try again later."
            }
            else -> if (isRussian) {
                "Не удалось удалить устройство. Обновите список и попробуйте снова."
            } else {
                "Could not remove this device. Refresh the list and try again."
            }
        }
    }
}
