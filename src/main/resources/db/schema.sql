CREATE DATABASE IF NOT EXISTS subastix_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE subastix_db;

CREATE TABLE IF NOT EXISTS usuarios (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  rol ENUM('COMPRADOR', 'VENDEDOR', 'ADMIN') NOT NULL DEFAULT 'COMPRADOR',
  saldo_disponible DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categorias (
  id_categoria INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE,
  descripcion TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS productos (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  id_vendedor INT NOT NULL,
  id_categoria INT NOT NULL,
  titulo VARCHAR(150) NOT NULL,
  descripcion TEXT NOT NULL,
  imagen_url VARCHAR(255),
  estado_producto ENUM('NUEVO', 'USADO_EXCELENTE', 'USADO_BUENO') NOT NULL DEFAULT 'NUEVO',
  CONSTRAINT fk_productos_vendedor FOREIGN KEY (id_vendedor)
    REFERENCES usuarios(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_productos_categoria FOREIGN KEY (id_categoria)
    REFERENCES categorias(id_categoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subastas (
  id_subasta INT AUTO_INCREMENT PRIMARY KEY,
  id_producto INT NOT NULL UNIQUE,
  precio_base DECIMAL(10,2) NOT NULL,
  precio_actual DECIMAL(10,2) NOT NULL,
  incremento_minimo DECIMAL(10,2) NOT NULL,
  fecha_inicio DATETIME NOT NULL,
  fecha_fin DATETIME NOT NULL,
  estado ENUM('PROGRAMADA', 'ACTIVA', 'FINALIZADA', 'CANCELADA') NOT NULL DEFAULT 'PROGRAMADA',
  id_ganador_actual INT NULL,
  version INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_subastas_producto FOREIGN KEY (id_producto)
    REFERENCES productos(id_producto) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_subastas_ganador FOREIGN KEY (id_ganador_actual)
    REFERENCES usuarios(id_usuario) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT chk_subastas_precio_base CHECK (precio_base > 0),
  CONSTRAINT chk_subastas_incremento CHECK (incremento_minimo > 0),
  CONSTRAINT chk_subastas_fechas CHECK (fecha_fin > fecha_inicio),
  KEY idx_subasta_estado_fecha (estado, fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pujas (
  id_puja INT AUTO_INCREMENT PRIMARY KEY,
  id_subasta INT NOT NULL,
  id_usuario INT NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  fecha_puja TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_pujas_subasta FOREIGN KEY (id_subasta)
    REFERENCES subastas(id_subasta) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pujas_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuarios(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_pujas_monto CHECK (monto > 0),
  KEY idx_pujas_subasta_monto (id_subasta, monto DESC),
  KEY idx_pujas_usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO categorias (nombre, descripcion) VALUES
  ('Tecnología y Laptops', 'Laptops, smartphones y componentes informáticos'),
  ('Consolas y Videojuegos', 'Consolas de última generación y accesorios'),
  ('Coleccionables', 'Artículos retro, figuras y memorabilia')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);
