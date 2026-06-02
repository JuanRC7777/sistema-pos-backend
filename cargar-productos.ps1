$tabla = "pos-productos"

$productos = @(
    @{ id="lac-001"; codigo="100001"; nombre="Leche entera 1L";       descripcion="Leche entera pasteurizada 1 litro";  precio="1.20"; stock="50"  },
    @{ id="lac-002"; codigo="100002"; nombre="Queso fresco 500g";      descripcion="Queso fresco artesanal 500 gramos";  precio="2.80"; stock="30"  },
    @{ id="lac-003"; codigo="100003"; nombre="Yogur natural 200g";     descripcion="Yogur natural sin azucar 200 gramos";precio="0.90"; stock="40"  },
    @{ id="beb-001"; codigo="200001"; nombre="Agua sin gas 500ml";     descripcion="Agua purificada sin gas 500ml";      precio="0.50"; stock="100" },
    @{ id="beb-002"; codigo="200002"; nombre="Coca Cola 600ml";        descripcion="Bebida gaseosa Coca Cola 600ml";     precio="1.00"; stock="80"  },
    @{ id="beb-003"; codigo="200003"; nombre="Jugo de naranja 1L";     descripcion="Jugo de naranja natural 1 litro";    precio="1.50"; stock="60"  },
    @{ id="car-001"; codigo="300001"; nombre="Pechuga de pollo 1kg";   descripcion="Pechuga de pollo fresca 1 kilogramo";precio="4.50"; stock="25"  },
    @{ id="car-002"; codigo="300002"; nombre="Carne molida 500g";      descripcion="Carne de res molida 500 gramos";     precio="3.20"; stock="20"  },
    @{ id="car-003"; codigo="300003"; nombre="Chuleta de cerdo 1kg";   descripcion="Chuleta de cerdo fresca 1 kilogramo";precio="5.00"; stock="15"  },
    @{ id="gra-001"; codigo="400001"; nombre="Arroz 1kg";              descripcion="Arroz blanco de grano largo 1kg";    precio="0.80"; stock="120" },
    @{ id="gra-002"; codigo="400002"; nombre="Frijoles negros 500g";   descripcion="Frijoles negros secos 500 gramos";   precio="1.10"; stock="90"  },
    @{ id="gra-003"; codigo="400003"; nombre="Lentejas 500g";          descripcion="Lentejas verdes secas 500 gramos";   precio="0.95"; stock="70"  },
    @{ id="alc-001"; codigo="500001"; nombre="Cerveza 330ml";          descripcion="Cerveza rubia lager 330ml";          precio="1.80"; stock="60"  },
    @{ id="alc-002"; codigo="500002"; nombre="Vino tinto 750ml";       descripcion="Vino tinto reserva 750ml";           precio="8.50"; stock="20"  },
    @{ id="alc-003"; codigo="500003"; nombre="Ron 750ml";              descripcion="Ron anejo 750ml";                    precio="12.00"; stock="15" }
)

foreach ($p in $productos) {
    # Item principal del producto
    aws dynamodb put-item --table-name $tabla --item "{
        `"PK`":{`"S`":`"PROD#$($p.id)`"},
        `"SK`":{`"S`":`"METADATA`"},
        `"codigo`":{`"S`":`"$($p.codigo)`"},
        `"nombre`":{`"S`":`"$($p.nombre)`"},
        `"descripcion`":{`"S`":`"$($p.descripcion)`"},
        `"precio`":{`"N`":`"$($p.precio)`"},
        `"stock`":{`"N`":`"$($p.stock)`"},
        `"activo`":{`"S`":`"true`"}
    }"

    # Índice por código
    aws dynamodb put-item --table-name $tabla --item "{
        `"PK`":{`"S`":`"CODIGO#$($p.codigo)`"},
        `"SK`":{`"S`":`"METADATA`"},
        `"productoId`":{`"S`":`"$($p.id)`"}
    }"

    Write-Host "Cargado: $($p.nombre) [$($p.codigo)]" -ForegroundColor Green
}

Write-Host "`nTodos los productos cargados exitosamente." -ForegroundColor Cyan
