# Daniel Montero 160004719
# Juan Ramirez 160004728


import matplotlib.pyplot as plt
import numpy as np

def funcion1(x):
	g = 9.8
	c = 15
	v = 35
	t = 9
	return (g*x/c)*(1 - np.exp(-1*(c/x)*t))-v


def funcion2(x):
	return np.sin(x) - x**2


def graficar(func, inicio, fin,funcion):
	x_vals =  np.linspace(inicio, fin, 400)
	y_vals = func(x_vals)
	plt.plot(x_vals, y_vals)
	plt.axhline(0, color='black', linewidth=1)
	plt.axvline(0, color='black', linewidth=1)
	plt.title(f"Método Gráfico {funcion}")
	plt.xlabel("x")
	plt.ylabel("f(x)")
	plt.grid(True)
	plt.show()


def aproximacion_metodo_grafico(func):

	n = 1
	print(f'x			f(x)', end='\n\n')
	while True:
		print(f'{n}			{func(n)}', end='\n\n')
		try:
			if (func(n-1) < 0 and func(n) >= 0) or (func(n-1) >= 0 and func(n) < 0):
				return [n-1, n]
		except:
			pass # En caso de una indeterminacion
		finally:
			n += 1


def aproximacion_metodo_biseccion(x1, x2, func, es):
	xi, xu = x1, x2
	n = 1
	xr = None
	xr_prev = None

	while True:
		if n > 1:
			xr_prev = xr

		xr = (xi + xu)/2
		evaluacion = func(xi) * func(xr)

		print(f'{n}) xr = ({xi} + {xu})/2 = {xr}')
		print(f"	f({xi}) * f({xr})", "> 0" if evaluacion > 0 else "< 0" if evaluacion < 0 else " = 0", end="\n\n")

		if evaluacion == 0 or (n > 1 and validacion_error_normalizado(xr, xr_prev, es)):
			return xr
		elif evaluacion < 0:
			xu = xr
		elif evaluacion > 0:
			xi = xr

		n += 1


def validacion_error_normalizado(xr_nuevo, xr_anterior, es):
	return abs((xr_nuevo-xr_anterior)/xr_nuevo) * 100 < es


def punto1():
	graficar(funcion1, 0, 62, "f(X) = (9.8*X/15)*(1-e^-(15*9/X))-35")

	print("MÉTODO GRÁFICO: ", end="\n\n")

	x1, x2 = aproximacion_metodo_grafico(funcion1)
	print(f"INTERVALO: [{x1},{x2}]", end='\n\n\n')

	print("MÉTODO BISECCIÓN: ", end="\n\n")

	rta_xr = aproximacion_metodo_biseccion(x1, x2, funcion1, es=0.1)
	print(f"LA RAIZ RESULTANTE ES: {rta_xr}", end="\n\n\n")


def punto2():
	graficar(funcion2, 0, 1, "f(X) = sen(X) - X^2")
	print("MÉTODO GRÁFICO: ", end="\n\n")

	x1, x2 = aproximacion_metodo_grafico(funcion2)
	print(f"INTERVALO: [{x1},{x2}]", end='\n\n\n')

	print("MÉTODO BISECCIÓN: ", end="\n\n")

	rta_xr = aproximacion_metodo_biseccion(x1=0.5, x2=1, func=funcion2, es=1)
	print(f"LA RAIZ RESULTANTE ES: {rta_xr}")


def main():
	punto1()
	punto2()


if __name__ == '__main__':
	main()

