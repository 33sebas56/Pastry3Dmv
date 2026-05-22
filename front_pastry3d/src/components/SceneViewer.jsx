import { Suspense, useEffect, useMemo } from "react";
import { Canvas } from "@react-three/fiber";
import { Bounds, Center, Environment, OrbitControls, useGLTF } from "@react-three/drei";
import { getAssetUrl } from "../api/apiClient";

function parseJson(value, fallback) {
  if (!value) return fallback;
  if (typeof value === "object") return value;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function vectorFrom(value, fallback = [0, 0, 0]) {
  if (!value) return fallback;
  if (Array.isArray(value)) return value;
  return [Number(value.x || 0), Number(value.y || 0), Number(value.z || 0)];
}

function applyColorOverrides(object, colorOverrides = {}) {
  if (!object || !colorOverrides) return;

  object.traverse((child) => {
    if (!child.isMesh || !child.material) return;

    const materials = Array.isArray(child.material) ? child.material : [child.material];

    materials.forEach((material) => {
      if (!material || !material.color) return;

      const materialName = material.name || "primary";
      const override =
        colorOverrides[materialName] ||
        colorOverrides[materialName.toLowerCase()] ||
        colorOverrides.primary ||
        colorOverrides.petals;

      if (override) {
        material = material.clone();
        material.color.set(override);
        child.material = material;
      }
    });
  });
}

function ModelItem({ modelUrl, position, rotation, scale = 1, colorOverrides }) {
  const url = getAssetUrl(modelUrl);
  const gltf = useGLTF(url);

  const clonedScene = useMemo(() => gltf.scene.clone(true), [gltf.scene]);

  useEffect(() => {
    applyColorOverrides(clonedScene, colorOverrides);
  }, [clonedScene, colorOverrides]);

  return (
    <primitive
      object={clonedScene}
      position={vectorFrom(position)}
      rotation={vectorFrom(rotation)}
      scale={Number(scale || 1)}
    />
  );
}

function SceneContent({ scene }) {
  const base = scene?.base;
  const parts = Array.isArray(scene?.parts) ? scene.parts : [];

  if (scene?.modelUrl) {
    return <ModelItem modelUrl={scene.modelUrl} position={[0, 0, 0]} rotation={[0, 0, 0]} scale={1} />;
  }

  return (
    <Bounds fit clip observe margin={1.25}>
      <Center>
        {base?.modelUrl && (
          <ModelItem
            modelUrl={base.modelUrl}
            position={base.position || [0, 0, 0]}
            rotation={base.rotation || [0, 0, 0]}
            scale={base.scale || 1}
          />
        )}

        {parts.map((part, index) => (
          <ModelItem
            key={`${part.assetKey || "part"}-${index}`}
            modelUrl={part.modelUrl}
            position={part.position || [0, 1.15, 0]}
            rotation={part.rotation || [0, 0, 0]}
            scale={part.scale || 0.35}
            colorOverrides={part.colorOverrides || {}}
          />
        ))}
      </Center>
    </Bounds>
  );
}

function ViewerFallback() {
  return (
    <div className="viewer-fallback">
      <div className="spinner" />
      <span>Cargando modelos 3D...</span>
    </div>
  );
}

export default function SceneViewer({ sceneJson, modelUrl, status }) {
  const scene = parseJson(sceneJson, {});
  const hasScene = scene?.base || scene?.modelUrl || modelUrl;

  const normalizedScene = modelUrl && !scene?.base && !scene?.modelUrl ? { modelUrl } : scene;

  if (!hasScene || status === "PENDING_MANUAL_MODEL") {
    return (
      <section className="viewer-empty">
        <h3>Modelo pendiente</h3>
        <p>
          La receta se generó, pero todavía falta agregar uno o más modelos GLB a la biblioteca para construir la escena.
        </p>
      </section>
    );
  }

  return (
    <section className="viewer-card">
      <Canvas camera={{ position: [3.5, 2.5, 4], fov: 42 }}>
        <ambientLight intensity={0.7} />
        <directionalLight position={[3, 5, 4]} intensity={1.4} />
        <Suspense fallback={null}>
          <SceneContent scene={normalizedScene} />
          <Environment preset="studio" />
        </Suspense>
        <OrbitControls enablePan={false} minDistance={2} maxDistance={8} />
      </Canvas>
      <Suspense fallback={<ViewerFallback />} />
    </section>
  );
}